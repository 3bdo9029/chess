package chess;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {

    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece; // may be null

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
        this.promotionPiece = promotionPiece;
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
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ChessMove other)
                && this.startPosition.equals(other.startPosition)
                && this.endPosition.equals(other.endPosition)
                && this.promotionPiece == other.promotionPiece;
    }

    @Override
    public int hashCode() {
        int result = startPosition.hashCode();
        result = 31 * result + endPosition.hashCode();
        result = 31 * result + (promotionPiece == null ? 0 : promotionPiece.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return startPosition + " -> " + endPosition + (promotionPiece == null ? "" : " = " + promotionPiece);
    }
}
