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

    // Default constructor
    // Board starts empty (all null squares)
    public ChessBoard() {
    }

    /**
     * Creates a shallow clone of the given chessboard
     *
     * @param other the board to clone
     */
    public ChessBoard(ChessBoard other) {
        if (other == null) {
            throw new IllegalArgumentException("other board cannot be null");
        }
        for (int r = 0; r < 8; r++) {
            System.arraycopy(other.squares[r], 0, this.squares[r], 0, 8);
        }
    }

    /**
     * Finds the first occurrence of a matching piece on the board.
     * Returns null if not found.
     */
    public ChessPosition findPiece(ChessPiece target) {
        if (target == null) return null;

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = squares[row - 1][col - 1];
                if (piece == null) continue;

                if (piece.getTeamColor() == target.getTeamColor()
                        && piece.getPieceType() == target.getPieceType()) {
                    return new ChessPosition(row, col);
                }
            }
        }
        return null;
    }

    /**
     * Moves a piece from the start position to the end position.
     * Captures are handled by overwriting the destination square.
     * Promotion is handled if the move includes a promotion piece.
     */
    public void movePiece(ChessMove move) {
        if (move == null) {
            throw new IllegalArgumentException("move cannot be null");
        }

        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();

        ChessPiece moving = getPiece(start);
        if (moving == null) {
            return;
        }

        // Handle promotion (if present)
        if (move.getPromotionPiece() != null && moving.getPieceType() == ChessPiece.PieceType.PAWN) {
            moving = new ChessPiece(moving.getTeamColor(), move.getPromotionPiece());
        }

        addPiece(end, moving);
        addPiece(start, null);
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

        // Convert 1-based row/col into 0-based array index and store the piece
        squares[position.getRow() - 1][position.getColumn() - 1] = piece;
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

        // Convert 1-based row/col into 0-based array index and return the piece
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        // Clear anything currently on the board
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

    // Clears the board by setting every square to null
    private void clear() {
        for (int r = 0; r < 8; r++) {
            Arrays.fill(squares[r], null);
        }
    }

    // Places 8 pawns across the given row for the given color
    private void placePawns(int row, ChessGame.TeamColor color) {
        for (int col = 1; col <= 8; col++) {
            addPiece(new ChessPosition(row, col), new ChessPiece(color, ChessPiece.PieceType.PAWN));
        }
    }

    // Places the back rank (rook, knight, bishop, queen, king, bishop, knight, rook) for the given color
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
        // Only boards can be equal to boards
        if (!(o instanceof ChessBoard other)) return false;

        // Compare the 2D piece grid
        return Arrays.deepEquals(this.squares, other.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }

    @Override
    public String toString() {
        // Build a printable board from rank 8 down to rank 1
        StringBuilder sb = new StringBuilder();

        // Ranks (rows) 8 -> 1
        for (int r = 8; r >= 1; r--) {
            // Print the rank label
            sb.append(r).append(" ");

            for (int c = 1; c <= 8; c++) {
                ChessPiece p = squares[r - 1][c - 1];
                sb.append(p == null ? "." : p.toString()).append(" ");
            }

            sb.append('\n');
        }

        sb.append("  a b c d e f g h");
        return sb.toString();
    }
}
