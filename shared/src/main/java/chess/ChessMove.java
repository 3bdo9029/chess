package chess;

/**
 * Represents moving a chess piece on a chessboard.
 *
 * Note: You can add to this class, but you may not alter
 * the signature of the existing methods.
 */
public class ChessMove {

    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        if (startPosition == null) {
            throw new IllegalArgumentException("startPosition cannot be null");
        }
        if (endPosition == null) {
            throw new IllegalArgumentException("endPosition cannot be null");
        }

        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece; // may be null when no promotion
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return startPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return endPosition;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this move.
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }
}
