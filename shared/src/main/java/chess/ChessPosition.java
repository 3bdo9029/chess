package chess;

/**
 * Represents a single square position on a chess board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPosition {

    private static final int MIN = 1;
    private static final int MAX = 8;

    // Pack row and col into one 16-bit value: high nibble = row, low nibble = col.
    private final short packed;

    public ChessPosition(int row, int col) {
        requireOnBoard(row, col);
        this.packed = (short) ((row << 4) | col);
    }

    /**
     * @return which row this position is in
     * 1 codes for the bottom row
     */
    public int getRow() {
        return (packed >>> 4) & 0xF;
    }

    /**
     * @return which column this position is in
     * 1 codes for the left row
     */
    public int getColumn() {
        return packed & 0xF;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ChessPosition other) && this.packed == other.packed;
    }

    @Override
    public int hashCode() {
        return Short.hashCode(packed);
    }

    @Override
    public String toString() {
        return "(" + getRow() + ", " + getColumn() + ")";
    }

    private static void requireOnBoard(int row, int col) {
        if (row < MIN || row > MAX || col < MIN || col > MAX) {
            throw new IllegalArgumentException("Row/col must be in 1..8: (" + row + ", " + col + ")");
        }
    }
}
