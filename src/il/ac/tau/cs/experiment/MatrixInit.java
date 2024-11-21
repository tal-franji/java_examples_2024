package il.ac.tau.cs.experiment;

public class MatrixInit {
    public static class MemEfficientMatrix {
        private double[] data;
        private int rows = -1;
        private int cols = -1;

        public MemEfficientMatrix(int rows, int cols) {
            this.cols = cols;
            this.rows = rows;
            data = new double[rows * cols];
        }
        public double get(int x, int y) {
            return data[y * cols + x];
        }
        public void put(int x, int y, double value) {
            data[y * cols + x] = value;
        }
        public int getRows() {
            return rows;
        }
        public int getCols() {
            return cols;
        }
    }

    public static void multTableXY(MemEfficientMatrix M) {
        for (int x = 0; x < M.getCols(); x++) {
            for (int y = 0; y < M.getRows(); y++) {
                M.put(x, y, (double)(x * y));
            }
        }
    }
    public static void multTableYX(MemEfficientMatrix M) {
        for (int y = 0; y < M.getRows(); y++) {
            for (int x = 0; x < M.getCols(); x++) {
                M.put(x, y, (double)(x * y));
            }
        }
    }
    public static void main(String[] args) {
        final int M = 200;
        final int N = 200;
        var matrix = new MemEfficientMatrix(N, M);
        ArithInterpreter.timeIt(() -> {  //@200x200: 10224; @800x800 176687
            multTableYX(matrix);
        });
        ArithInterpreter.timeIt(() -> {  //@200x200: 19371; @800x800 629786
            multTableXY(matrix);
        });
    }

}
