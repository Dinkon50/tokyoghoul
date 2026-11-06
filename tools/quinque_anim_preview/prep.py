"""Готовит файлы для предпросмотра: сетки квинке (.tgqm -> .bin), палитра и скин CCG в папку assets."""
import os, shutil, struct
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'tokyoghoulrpg')
os.makedirs(os.path.join(HERE, 'assets'), exist_ok=True)
for name in ['quinque_kura', 'quinque_jason', 'quinque_narukami', 'quinque_narukami_open', 'quinque_ixa']:
    b = open(os.path.join(R, 'quinque', name + '.tgqm'), 'rb').read()
    o = 8; n, = struct.unpack('>i', b[o:o + 4]); o += 4; quads = []
    for _ in range(n):
        l, = struct.unpack('>H', b[o:o + 2]); o += 2 + l
        qc, = struct.unpack('>i', b[o + 12:o + 16]); o += 16
        quads.append(np.frombuffer(b[o:o + qc * 92], dtype='>f4').astype('<f4').reshape(qc, 23)); o += qc * 92
    q = np.concatenate(quads); v = q[:, :20].reshape(-1, 4, 5)
    tri = np.concatenate([v[:, [0, 1, 2]], v[:, [0, 2, 3]]], axis=1).reshape(-1, 5)
    np.concatenate([tri, np.repeat(q[:, 20:23], 6, axis=0)], axis=1).astype('<f4').tofile(os.path.join(HERE, 'assets', name + '.bin'))
shutil.copy(os.path.join(R, 'textures', 'quinque', 'palette.png'), os.path.join(HERE, 'assets', 'palette.png'))
shutil.copy(os.path.join(R, 'textures', 'skin', 'ccg.png'), os.path.join(HERE, 'assets', 'ccg.png'))
print('ok')
