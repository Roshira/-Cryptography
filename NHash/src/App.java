import java.nio.ByteBuffer;

// Назва публічного класу збігається з назвою файлу (App.java)
public class App {

    public static void main(String[] args) {
        System.out.println("=== Реалізація хеш-функції N-Hash ===");
        
        NHash hasher = new NHash();
        
        String input1 = "Hello, World!";
        String input2 = "Лабораторна робота N-Hash";
        
        byte[] hash1 = hasher.digest(input1.getBytes());
        byte[] hash2 = hasher.digest(input2.getBytes());
        
        System.out.println("Текст: " + input1);
        System.out.println("N-Hash: " + NHash.bytesToHex(hash1));
        System.out.println();
        
        System.out.println("Текст: " + input2);
        System.out.println("N-Hash: " + NHash.bytesToHex(hash2));
    }
}

// Клас, який реалізує сам алгоритм (без слова public)
class NHash {
    private int[] H;

    public NHash() {
        reset();
    }

    public void reset() {
        H = new int[]{0, 0, 0, 0};
    }

    private byte rot2(byte x) {
        int v = x & 0xFF;
        return (byte) (((v << 2) | (v >>> 6)) & 0xFF);
    }

    private byte S0(byte a, byte b) {
        int sum = ((a & 0xFF) + (b & 0xFF)) & 0xFF;
        return rot2((byte) sum);
    }

    private byte S1(byte a, byte b) {
        int sum = ((a & 0xFF) + (b & 0xFF) + 1) & 0xFF;
        return rot2((byte) sum);
    }

    private int f(int alpha, int beta) {
        byte a0 = (byte) (alpha >>> 24);
        byte a1 = (byte) (alpha >>> 16);
        byte a2 = (byte) (alpha >>> 8);
        byte a3 = (byte) (alpha);

        byte b0 = (byte) (beta >>> 24);
        byte b1 = (byte) (beta >>> 16);
        byte b2 = (byte) (beta >>> 8);
        byte b3 = (byte) (beta);

        byte t0 = (byte) (a0 ^ b0);
        byte t1 = (byte) (a1 ^ a0 ^ b1);
        byte t2 = (byte) (a2 ^ a3 ^ b2);
        byte t3 = (byte) (a3 ^ b3);

        byte y1 = S1(t1, t2);
        byte y0 = S0(t0, y1);
        byte y2 = S0(t2, y1);
        byte y3 = S1(t3, y2);

        return ((y0 & 0xFF) << 24) | ((y1 & 0xFF) << 16) | ((y2 & 0xFF) << 8) | (y3 & 0xFF);
    }

    private int[] EXG(int[] Z) {
        return new int[]{Z[2], Z[3], Z[0], Z[1]};
    }

    private int[] PS(int[] X, int[] P) {
        int L0 = P[0], L1 = P[1];
        int R0 = P[2], R1 = P[3];

        int fOut0 = f(R0 ^ X[0], R1 ^ X[1]);
        int fOut1 = f(R1 ^ X[2], R0 ^ X[3]);

        int newL0 = R0;
        int newL1 = R1;
        int newR0 = L0 ^ fOut0;
        int newR1 = L1 ^ fOut1;

        return new int[]{newL0, newL1, newR0, newR1};
    }

    private void compress(int[] M) {

        int[][] V = new int[9][4]; 
        
        int[] exgH = EXG(H);
        int[] P = new int[4];
        
        for (int j = 0; j < 4; j++) {
            P[j] = exgH[j] ^ V[0][j] ^ M[j];
        }

        for (int k = 1; k <= 8; k++) {
            int[] X = new int[4];
            for (int j = 0; j < 4; j++) {
                X[j] = V[k][j] ^ H[j];
            }
            P = PS(X, P);
        }

        for (int j = 0; j < 4; j++) {
            H[j] = M[j] ^ H[j] ^ P[j];
        }
    }

    public byte[] digest(byte[] message) {
        reset(); 

        int len = message.length;
        int padLen = 16 - (len % 16);
        byte[] padded = new byte[len + padLen];
        System.arraycopy(message, 0, padded, 0, len);
        padded[len] = (byte) 0x80;

        ByteBuffer buffer = ByteBuffer.wrap(padded);
        while (buffer.hasRemaining()) {
            int[] M = new int[4];
            for(int i = 0; i < 4; i++) {
                M[i] = buffer.getInt();
            }
            compress(M);
        }

        ByteBuffer out = ByteBuffer.allocate(16);
        for(int i = 0; i < 4; i++) {
            out.putInt(H[i]);
        }
        return out.array();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}