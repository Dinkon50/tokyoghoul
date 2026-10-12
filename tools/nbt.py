"""Минимальное чтение и запись NBT (формат файлов построек Minecraft), без сторонних библиотек."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Tag:
    """Значение с явным типом NBT: Tag(INT, 5), Tag(LIST, [...], item_type=COMPOUND)."""
    def __init__(self, kind, value, item_type=None):
        self.kind, self.value, self.item_type = kind, value, item_type

    def __repr__(self):
        return f"Tag({self.kind}, {self.value!r})"


def _read(buf, pos, kind):
    if kind == BYTE:
        return struct.unpack_from(">b", buf, pos)[0], pos + 1
    if kind == SHORT:
        return struct.unpack_from(">h", buf, pos)[0], pos + 2
    if kind == INT:
        return struct.unpack_from(">i", buf, pos)[0], pos + 4
    if kind == LONG:
        return struct.unpack_from(">q", buf, pos)[0], pos + 8
    if kind == FLOAT:
        return struct.unpack_from(">f", buf, pos)[0], pos + 4
    if kind == DOUBLE:
        return struct.unpack_from(">d", buf, pos)[0], pos + 8
    if kind == BYTE_ARRAY:
        n = struct.unpack_from(">i", buf, pos)[0]; pos += 4
        return list(buf[pos:pos + n]), pos + n
    if kind == STRING:
        n = struct.unpack_from(">H", buf, pos)[0]; pos += 2
        return buf[pos:pos + n].decode("utf-8"), pos + n
    if kind == LIST:
        item = buf[pos]; n = struct.unpack_from(">i", buf, pos + 1)[0]; pos += 5
        out = []
        for _ in range(n):
            v, pos = _read(buf, pos, item)
            out.append(v)
        return ("list", item, out), pos
    if kind == COMPOUND:
        out = {}
        while True:
            t = buf[pos]; pos += 1
            if t == END:
                return out, pos
            name, pos = _read(buf, pos, STRING)
            v, pos = _read(buf, pos, t)
            out[name] = (t, v)
    if kind == INT_ARRAY:
        n = struct.unpack_from(">i", buf, pos)[0]; pos += 4
        return list(struct.unpack_from(f">{n}i", buf, pos)), pos + 4 * n
    if kind == LONG_ARRAY:
        n = struct.unpack_from(">i", buf, pos)[0]; pos += 4
        return list(struct.unpack_from(f">{n}q", buf, pos)), pos + 8 * n
    raise ValueError(kind)


def load(path):
    buf = gzip.open(path, "rb").read()
    assert buf[0] == COMPOUND
    name, pos = _read(buf, 1, STRING)
    root, _ = _read(buf, pos, COMPOUND)
    return root


def plain(v):
    """Убрать типы для удобного просмотра."""
    if isinstance(v, tuple) and len(v) == 2 and isinstance(v[0], int):
        return plain(v[1])
    if isinstance(v, tuple) and v and v[0] == "list":
        return [plain(x) for x in v[2]]
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    return v


def _write(out, tag):
    k, v = tag.kind, tag.value
    if k == BYTE:
        out += struct.pack(">b", v)
    elif k == SHORT:
        out += struct.pack(">h", v)
    elif k == INT:
        out += struct.pack(">i", v)
    elif k == LONG:
        out += struct.pack(">q", v)
    elif k == FLOAT:
        out += struct.pack(">f", v)
    elif k == DOUBLE:
        out += struct.pack(">d", v)
    elif k == STRING:
        b = v.encode("utf-8"); out += struct.pack(">H", len(b)) + b
    elif k == LIST:
        item = tag.item_type if tag.item_type is not None else (v[0].kind if v else END)
        out += struct.pack(">bi", item, len(v))
        for x in v:
            assert x.kind == item, (x.kind, item)
            _write(out, x)
    elif k == COMPOUND:
        for name, x in v.items():
            out += struct.pack(">b", x.kind)
            _write(out, Tag(STRING, name))
            _write(out, x)
        out += struct.pack(">b", END)
    elif k == INT_ARRAY:
        out += struct.pack(">i", len(v)) + struct.pack(f">{len(v)}i", *v)
    else:
        raise ValueError(k)


def save(path, root):
    out = bytearray()
    out += struct.pack(">b", COMPOUND)
    _write(out, Tag(STRING, ""))
    _write(out, Tag(COMPOUND, root))
    with gzip.open(path, "wb") as f:
        f.write(bytes(out))
