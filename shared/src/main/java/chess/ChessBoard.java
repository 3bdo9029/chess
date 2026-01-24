package chess;

import java.util.Arrays;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {

    // Board indexed as [row-1][col-1]
    private final ChessPiece[][] squares = new ChessPiece[8][8];

    public ChessBoard() {
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null");
        }
        squares[position.getRow() - 1][position.getColumn() - 1] = piece; // piece may be null
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null");
        }
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        clear();

        // White pieces (bottom: rows 1-2)
        placeBackRank(1, ChessGame.TeamColor.WHITE);
        placePawns(2, ChessGame.TeamColor.WHITE);

        // Black pieces (top: rows 8-7)
        placeBackRank(8, ChessGame.TeamColor.BLACK);
        placePawns(7, ChessGame.TeamColor.BLACK);
    }

    // -----------------------
    // Helpers (added)
    // -----------------------

    private void clear() {
        for (int r = 0; r < 8; r++) {
            Arrays.fill(squares[r], null);
        }
    }

    private void placePawns(int row, ChessGame.TeamColor color) {
        for (int col = 1; col <= 8; col++) {
            addPiece(new ChessPosition(row, col), new ChessPiece(color, ChessPiece.PieceType.PAWN));
        }
    }

    private void placeBackRank(int row, ChessGame.TeamColor color) {
        addPiece(new ChessPosition(row, 1), new ChessPiece(color, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(row, 2), new ChessPiece(color, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(row, 3), new ChessPiece(color, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(row, 4), new ChessPiece(color, ChessPiece.PieceType.QUEEN));
        addPiece(new ChessPosition(row, 5), new ChessPiece(color, ChessPiece.PieceType.KING));
        addPiece(new ChessPosition(row, 6), new ChessPiece(color, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(row, 7), new ChessPiece(color, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(row, 8), new ChessPiece(color, ChessPiece.PieceType.ROOK));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessBoard other)) return false;
        return Arrays.deepEquals(this.squares, other.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 8; r >= 1; r--) {
            sb.append(r).append(" ");
            for (int c = 1; c <= 8; c++) {
                ChessPiece p = getPiece(new ChessPosition(r, c));
                sb.append(p == null ? "." : p.toString()).append(" ");
            }
            sb.append('\n');
        }
        sb.append("  a b c d e f g h");
        return sb.toString();
    }
}
