package chess;

import java.util.Arrays;

/**
 * 
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {

        private ChessPiece[][] board;

    public ChessBoard() {
                board = new ChessPiece[8][8];
        
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
    }        if (position == null) return;
        board[position.getRow() - 1][position.getColumn() - 1] = piece;

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
    }        if (position == null) return null;
        return board[position.getRow() - 1][position.getColumn() - 1];

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
    }        board = new ChessPiece[8][8];

        // White pieces (row 1) and pawns (row 2)
        board[0] = backRow(ChessGame.TeamColor.WHITE);
        board[1] = pawnRow(ChessGame.TeamColor.WHITE);

        // Black pieces (row 8) and pawns (row 7)
        board[7] = backRow(ChessGame.TeamColor.BLACK);
        board[6] = pawnRow(ChessGame.TeamColor.BLACK);

        private ChessPiece[] pawnRow(ChessGame.TeamColor color) {
        ChessPiece[] row = new ChessPiece[8];
        for (int i = 0; i < 8; i++) {
            row[i] = new ChessPiece(color, ChessPiece.PieceType.PAWN);
        }
        return row;
    }

        private ChessPiece[] backRow(ChessGame.TeamColor color) {
        return new ChessPiece[]{
                new ChessPiece(color, ChessPiece.PieceType.ROOK),
                new ChessPiece(color, ChessPiece.PieceType.KNIGHT),
                new ChessPiece(color, ChessPiece.PieceType.BISHOP),
                new ChessPiece(color, ChessPiece.PieceType.QUEEN),
                new ChessPiece(color, ChessPiece.PieceType.KING),
                new ChessPiece(color, ChessPiece.PieceType.BISHOP),
                new ChessPiece(color, ChessPiece.PieceType.KNIGHT),
                new ChessPiece(color, ChessPiece.PieceType.ROOK),
        };
    }

        // Highly recommended for tests that compare boards
    @Override
    public boolean equals(Object o) {
                if (this == o) return true;
        if (!(o instanceof ChessBoard other)) return false;
        return Arrays.deepEquals(this.board, other.board);

            @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }
}
