package com.tokyoghoulrpg.client.quinque;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Сетка квинке из файла .tgqm (его делает tools/quinque_mesh.py из твоей модели Blockbench).
 * Части (кости Blockbench) хранятся отдельно: потом к ним подключим анимации.
 * Координаты в блоках, начало — точка хвата, модель вытянута вдоль +Y.
 */
public final class QuinqueMesh {
    /** Одна часть: точка вращения и четырёхугольники по 23 числа (4 x (x, y, z, u, v) + нормаль). */
    public record Part(String name, float pivotX, float pivotY, float pivotZ, float[] quads, int quadCount) {
    }

    private final List<Part> parts;

    private QuinqueMesh(List<Part> parts) {
        this.parts = parts;
    }

    public List<Part> parts() {
        return parts;
    }

    public static QuinqueMesh read(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(stream);
        byte[] magic = new byte[4];
        in.readFully(magic);
        if (magic[0] != 'T' || magic[1] != 'G' || magic[2] != 'Q' || magic[3] != 'M') {
            throw new IOException("не файл квинке");
        }
        int version = in.readInt();
        if (version != 1) {
            throw new IOException("неизвестная версия файла квинке: " + version);
        }
        int partCount = in.readInt();
        List<Part> parts = new ArrayList<>(partCount);
        for (int p = 0; p < partCount; p++) {
            String name = in.readUTF();
            float px = in.readFloat();
            float py = in.readFloat();
            float pz = in.readFloat();
            int quadCount = in.readInt();
            float[] quads = new float[quadCount * 23];
            for (int i = 0; i < quads.length; i++) {
                quads[i] = in.readFloat();
            }
            parts.add(new Part(name, px, py, pz, quads, quadCount));
        }
        return new QuinqueMesh(parts);
    }
}
