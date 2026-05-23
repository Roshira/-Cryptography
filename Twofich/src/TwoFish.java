public class TwoFish {
    public static void main(String[] args) {
        System.out.println("=== Реалізація алгоритму Twofish (256-bit) ===");

        Twofish256 twofish = new Twofish256();

        byte[] key = "ThisIsASecretKeyForTwofish256bit".getBytes();
        twofish.setKey(key);

        String plaintext = "Лабораторна робота: Алгоритм Twofish працює!";
        System.out.println("Оригінальний текст: " + plaintext);

        byte[] pt = plaintext.getBytes();
        byte[] ct = twofish.encrypt(pt);
        System.out.println("Зашифровано (HEX):  " + bytesToHex(ct));

        byte[] dt = twofish.decrypt(ct);
        System.out.println("Розшифровано:       " + new String(dt));
    }

   public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

class Twofish256 {
    private static final byte[] q0 = new byte[256];
    private static final byte[] q1 = new byte[256];

    static {
        int[] t0_0 = {0x8, 0x1, 0x7, 0xD, 0x6, 0xF, 0x3, 0x2, 0x0, 0xB, 0x5, 0x9, 0xE, 0xC, 0xA, 0x4};
        int[] t1_0 = {0xE, 0xC, 0xB, 0x8, 0x1, 0x2, 0x3, 0x5, 0xF, 0x4, 0xA, 0x6, 0x7, 0x0, 0x9, 0xD};
        int[] t2_0 = {0xB, 0xA, 0x5, 0xE, 0x6, 0xD, 0x9, 0x0, 0xC, 0x8, 0xF, 0x3, 0x2, 0x4, 0x7, 0x1};
        int[] t3_0 = {0xD, 0x7, 0xF, 0x4, 0x1, 0x2, 0x6, 0xE, 0x9, 0xB, 0x3, 0x0, 0x8, 0x5, 0xC, 0xA};

        int[] t0_1 = {0x2, 0x8, 0xB, 0xD, 0xF, 0x7, 0x6, 0xE, 0x3, 0x1, 0x9, 0x4, 0x0, 0xA, 0xC, 0x5};
        int[] t1_1 = {0x1, 0xE, 0x2, 0xB, 0x4, 0xC, 0x3, 0x7, 0x6, 0xD, 0xA, 0x5, 0xF, 0x9, 0x0, 0x8};
        int[] t2_1 = {0x4, 0xC, 0x7, 0x5, 0x1, 0x6, 0x9, 0xA, 0x0, 0xE, 0xD, 0x8, 0x2, 0xB, 0x3, 0xF};
        int[] t3_1 = {0xB, 0x9, 0x5, 0x1, 0xC, 0x3, 0xD, 0xE, 0x6, 0x4, 0x7, 0xF, 0x2, 0x0, 0x8, 0xA};

        for (int x = 0; x < 256; x++) {
            int a0 = (x >> 4) & 0xF;
            int b0 = x & 0xF;

            // Генерація q0
            int a1 = a0 ^ b0;
            int b1 = a0 ^ ((b0 >> 1) | ((b0 & 1) << 3)) ^ ((8 * a0) & 0xF);
            int a2 = t0_0[a1];
            int b2 = t1_0[b1];
            int a3 = a2 ^ b2;
            int b3 = a2 ^ ((b2 >> 1) | ((b2 & 1) << 3)) ^ ((8 * a2) & 0xF);
            int a4 = t2_0[a3];
            int b4 = t3_0[b3];
            q0[x] = (byte) ((b4 << 4) | a4);

            // Генерація q1
            a1 = a0 ^ b0;
            b1 = a0 ^ ((b0 >> 1) | ((b0 & 1) << 3)) ^ ((8 * a0) & 0xF);
            a2 = t0_1[a1];
            b2 = t1_1[b1];
            a3 = a2 ^ b2;
            b3 = a2 ^ ((b2 >> 1) | ((b2 & 1) << 3)) ^ ((8 * a2) & 0xF);
            a4 = t2_1[a3];
            b4 = t3_1[b3];
            q1[x] = (byte) ((b4 << 4) | a4);
        }
    }

    private int[] K; // Раундові ключі (40 слів)
    private int[] S; // S-блоки, залежні від ключа

    // Множення в полі Галуа GF(2^8)
    private static int gfMult(int a, int b, int poly) {
        int res = 0;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) res ^= a;
            a <<= 1;
            if ((a & 0x100) != 0) a ^= poly;
            b >>= 1;
        }
        return res & 0xFF;
    }

    // Множення на матрицю RS
    private int rsMult(byte[] m, int offset) {
        int m0 = m[offset] & 0xFF,     m1 = m[offset + 1] & 0xFF;
        int m2 = m[offset + 2] & 0xFF, m3 = m[offset + 3] & 0xFF;
        int m4 = m[offset + 4] & 0xFF, m5 = m[offset + 5] & 0xFF;
        int m6 = m[offset + 6] & 0xFF, m7 = m[offset + 7] & 0xFF;

        int s0 = gfMult(m0, 0x01, 0x14D) ^ gfMult(m1, 0xA4, 0x14D) ^ gfMult(m2, 0x55, 0x14D) ^ gfMult(m3, 0x87, 0x14D)
               ^ gfMult(m4, 0x5A, 0x14D) ^ gfMult(m5, 0x58, 0x14D) ^ gfMult(m6, 0xDB, 0x14D) ^ gfMult(m7, 0x9E, 0x14D);
        int s1 = gfMult(m0, 0xA4, 0x14D) ^ gfMult(m1, 0x56, 0x14D) ^ gfMult(m2, 0x82, 0x14D) ^ gfMult(m3, 0xF3, 0x14D)
               ^ gfMult(m4, 0x1E, 0x14D) ^ gfMult(m5, 0xC6, 0x14D) ^ gfMult(m6, 0x68, 0x14D) ^ gfMult(m7, 0xE5, 0x14D);
        int s2 = gfMult(m0, 0x02, 0x14D) ^ gfMult(m1, 0xA1, 0x14D) ^ gfMult(m2, 0xFC, 0x14D) ^ gfMult(m3, 0xC1, 0x14D)
               ^ gfMult(m4, 0x47, 0x14D) ^ gfMult(m5, 0xAE, 0x14D) ^ gfMult(m6, 0x3D, 0x14D) ^ gfMult(m7, 0x19, 0x14D);
        int s3 = gfMult(m0, 0xA4, 0x14D) ^ gfMult(m1, 0x55, 0x14D) ^ gfMult(m2, 0x87, 0x14D) ^ gfMult(m3, 0x5A, 0x14D)
               ^ gfMult(m4, 0x58, 0x14D) ^ gfMult(m5, 0xDB, 0x14D) ^ gfMult(m6, 0x9E, 0x14D) ^ gfMult(m7, 0x03, 0x14D);
        
        return s0 | (s1 << 8) | (s2 << 16) | (s3 << 24);
    }

    // Множення на матрицю MDS
    private int mdsMult(int X0, int X1, int X2, int X3) {
        int Z0 = X0 ^ gfMult(X1, 0xEF, 0x169) ^ gfMult(X2, 0x5B, 0x169) ^ gfMult(X3, 0x5B, 0x169);
        int Z1 = gfMult(X0, 0x5B, 0x169) ^ gfMult(X1, 0xEF, 0x169) ^ gfMult(X2, 0xEF, 0x169) ^ X3;
        int Z2 = gfMult(X0, 0xEF, 0x169) ^ gfMult(X1, 0x5B, 0x169) ^ X2 ^ gfMult(X3, 0xEF, 0x169);
        int Z3 = gfMult(X0, 0xEF, 0x169) ^ X1 ^ gfMult(X2, 0xEF, 0x169) ^ gfMult(X3, 0x5B, 0x169);
        return (Z0 & 0xFF) | ((Z1 & 0xFF) << 8) | ((Z2 & 0xFF) << 16) | ((Z3 & 0xFF) << 24);
    }

    // Функція h
    private int h(int X, int[] L) {
        int b0 = X & 0xFF, b1 = (X >>> 8) & 0xFF, b2 = (X >>> 16) & 0xFF, b3 = (X >>> 24) & 0xFF;

        b0 = q1[b0] & 0xFF; b1 = q0[b1] & 0xFF; b2 = q0[b2] & 0xFF; b3 = q1[b3] & 0xFF;
        b0 ^= (L[3] & 0xFF); b1 ^= ((L[3] >>> 8) & 0xFF); b2 ^= ((L[3] >>> 16) & 0xFF); b3 ^= ((L[3] >>> 24) & 0xFF);

        b0 = q1[b0] & 0xFF; b1 = q1[b1] & 0xFF; b2 = q0[b2] & 0xFF; b3 = q0[b3] & 0xFF;
        b0 ^= (L[2] & 0xFF); b1 ^= ((L[2] >>> 8) & 0xFF); b2 ^= ((L[2] >>> 16) & 0xFF); b3 ^= ((L[2] >>> 24) & 0xFF);

        b0 = q1[b0] & 0xFF; b1 = q0[b1] & 0xFF; b2 = q1[b2] & 0xFF; b3 = q0[b3] & 0xFF;
        b0 ^= (L[1] & 0xFF); b1 ^= ((L[1] >>> 8) & 0xFF); b2 ^= ((L[1] >>> 16) & 0xFF); b3 ^= ((L[1] >>> 24) & 0xFF);

        b0 = q0[b0] & 0xFF; b1 = q0[b1] & 0xFF; b2 = q1[b2] & 0xFF; b3 = q1[b3] & 0xFF;
        b0 ^= (L[0] & 0xFF); b1 ^= ((L[0] >>> 8) & 0xFF); b2 ^= ((L[0] >>> 16) & 0xFF); b3 ^= ((L[0] >>> 24) & 0xFF);

        b0 = q1[b0] & 0xFF; b1 = q0[b1] & 0xFF; b2 = q1[b2] & 0xFF; b3 = q0[b3] & 0xFF;

        return mdsMult(b0, b1, b2, b3);
    }

    private int g(int X) { return h(X, S); }
    private int rol(int val, int shift) { return (val << shift) | (val >>> (32 - shift)); }
    private int ror(int val, int shift) { return (val >>> shift) | (val << (32 - shift)); }

    // Конвертація байтів у int (Little Endian)
    private int bytesToInt(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8) | ((b[offset + 2] & 0xFF) << 16) | ((b[offset + 3] & 0xFF) << 24);
    }

    private void intToBytes(int val, byte[] b, int offset) {
        b[offset] = (byte) val; b[offset + 1] = (byte) (val >>> 8);
        b[offset + 2] = (byte) (val >>> 16); b[offset + 3] = (byte) (val >>> 24);
    }

    // Розширення ключа
    public void setKey(byte[] key) {
        if (key.length != 32) throw new IllegalArgumentException("Ключ повинен бути 256 біт (32 байти)");

        int[] Me = new int[4], Mo = new int[4];
        for (int i = 0; i < 4; i++) {
            Me[i] = bytesToInt(key, i * 8);
            Mo[i] = bytesToInt(key, i * 8 + 4);
        }

        S = new int[4];
        for (int i = 0; i < 4; i++) S[i] = rsMult(key, i * 8);

        K = new int[40];
        for (int i = 0; i < 20; i++) {
            int A = h(i * 2 * 0x01010101, Me);
            int B = rol(h((i * 2 + 1) * 0x01010101, Mo), 8);
            K[2 * i] = A + B;
            K[2 * i + 1] = rol(A + B + B, 9);
        }
    }

    // Шифрування одного 128-бітного блоку
    public byte[] encryptBlock(byte[] in) {
        int p0 = bytesToInt(in, 0) ^ K[0], p1 = bytesToInt(in, 4) ^ K[1];
        int p2 = bytesToInt(in, 8) ^ K[2], p3 = bytesToInt(in, 12) ^ K[3];

        for (int r = 0; r < 16; r++) {
            int t0 = g(p0), t1 = g(rol(p1, 8));
            int f0 = t0 + t1 + K[2 * r + 8], f1 = t0 + 2 * t1 + K[2 * r + 9];
            p2 = ror(p2 ^ f0, 1);
            p3 = rol(p3, 1) ^ f1;

            if (r < 15) { int tmp = p0; p0 = p2; p2 = tmp; tmp = p1; p1 = p3; p3 = tmp; }
        }

        byte[] out = new byte[16];
        intToBytes(p0 ^ K[4], out, 0); intToBytes(p1 ^ K[5], out, 4);
        intToBytes(p2 ^ K[6], out, 8); intToBytes(p3 ^ K[7], out, 12);
        return out;
    }

    // Розшифрування одного 128-бітного блоку
    public byte[] decryptBlock(byte[] in) {
        int p0 = bytesToInt(in, 0) ^ K[4], p1 = bytesToInt(in, 4) ^ K[5];
        int p2 = bytesToInt(in, 8) ^ K[6], p3 = bytesToInt(in, 12) ^ K[7];

        for (int r = 15; r >= 0; r--) {
            int t0 = g(p0), t1 = g(rol(p1, 8));
            int f0 = t0 + t1 + K[2 * r + 8], f1 = t0 + 2 * t1 + K[2 * r + 9];
            p2 = rol(p2, 1) ^ f0;
            p3 = ror(p3 ^ f1, 1);

            if (r > 0) { int tmp = p0; p0 = p2; p2 = tmp; tmp = p1; p1 = p3; p3 = tmp; }
        }

        byte[] out = new byte[16];
        intToBytes(p0 ^ K[0], out, 0); intToBytes(p1 ^ K[1], out, 4);
        intToBytes(p2 ^ K[2], out, 8); intToBytes(p3 ^ K[3], out, 12);
        return out;
    }

    // PKCS7 Padding та режими шифрування довільного тексту
    public byte[] encrypt(byte[] in) {
        int padLen = 16 - (in.length % 16);
        byte[] padded = new byte[in.length + padLen];
        System.arraycopy(in, 0, padded, 0, in.length);
        for (int i = in.length; i < padded.length; i++) padded[i] = (byte) padLen;

        byte[] out = new byte[padded.length];
        for (int i = 0; i < padded.length; i += 16) {
            byte[] block = new byte[16];
            System.arraycopy(padded, i, block, 0, 16);
            System.arraycopy(encryptBlock(block), 0, out, i, 16);
        }
        return out;
    }

    public byte[] decrypt(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i += 16) {
            byte[] block = new byte[16];
            System.arraycopy(in, i, block, 0, 16);
            System.arraycopy(decryptBlock(block), 0, out, i, 16);
        }
        int padLen = out[out.length - 1] & 0xFF;
        byte[] unpadded = new byte[out.length - padLen];
        System.arraycopy(out, 0, unpadded, 0, unpadded.length);
        return unpadded;
    }
}