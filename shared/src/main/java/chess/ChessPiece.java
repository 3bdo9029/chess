package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private static final int MIN = 1;
    private static final int MAX = 8;

    private final ChessGame.TeamColor pieceColor;
    private final ChessPiece.PieceType type;

    // Useful for debugging; not required, but safe to include.
    private static final Map<ChessPiece.PieceType, Character> TYPE_TO_CHAR =
            Map.of(
                    PieceType.PAWN, 'p',
                    PieceType.KNIGHT, 'n',
                    PieceType.BISHOP, 'b',
                    PieceType.ROOK, 'r',
                    PieceType.QUEEN, 'q',
                    PieceType.KING, 'k'
            );

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        if (pieceColor == null) throw new IllegalArgumentException("pieceColor cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        if (board == null) throw new IllegalArgumentException("board cannot be null");
        if (myPosition == null) throw new IllegalArgumentException("myPosition cannot be null");

        Set<ChessMove> moves = new HashSet<>();

        switch (type) {
            case PAWN -> addPawnMoves(board, myPosition, moves);
            case KNIGHT -> addKnightMoves(board, myPosition, moves);
            case BISHOP -> addSlidingMoves(board, myPosition, moves, new int[][]{
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            });
            case ROOK -> addSlidingMoves(board, myPosition, moves, new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1}
            });
            case QUEEN -> addSlidingMoves(board, myPosition, moves, new int[][]{
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1}
            });
            case KING -> addKingMoves(board, myPosition, moves);
        }

        return moves;
    }

    // -----------------------
    // Move generation helpers
    // -----------------------

    private void addPawnMoves(ChessBoard board, ChessPosition from, Set<ChessMove> moves) {
        int r = from.getRow();
        int c = from.getColumn();

        int dir = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promotionRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        // Forward one
        int r1 = r + dir;
        if (onBoard(r1, c) && board.getPiece(new ChessPosition(r1, c)) == null) {
            ChessPosition to1 = new ChessPosition(r1, c);
            if (r1 == promotionRow) addPromotionMoves(from, to1, moves);
            else moves.add(new ChessMove(from, to1, null));

            // Forward two (only if forward one is clear)
            int r2 = r + 2 * dir;
            if (r == startRow && onBoard(r2, c) && board.getPiece(new ChessPosition(r2, c)) == null) {
                moves.add(new ChessMove(from, new ChessPosition(r2, c), null));
            }
        }

        // Diagonal captures
        for (int dc : new int[]{-1, 1}) {
            int rr = r + dir;
            int cc = c + dc;
            if (!onBoard(rr, cc)) continue;

            ChessPosition to = new ChessPosition(rr, cc);
            ChessPiece target = board.getPiece(to);
            if (target != null && target.getTeamColor() != this.pieceColor) {
                if (rr == promotionRow) addPromotionMoves(from, to, moves);
                else moves.add(new ChessMove(from, to, null));
            }
        }

        // En passant intentionally not handled here (requires game history).
    }

    private void addKnightMoves(ChessBoard board, ChessPosition from, Set<ChessMove> moves) {
        int r = from.getRow();
        int c = from.getColumn();

        int[][] deltas = {
                {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
        };

        for (int[] d : deltas) {
            tryAddStepMove(board, from, r + d[0], c + d[1], moves);
        }
    }

    private void addKingMoves(ChessBoard board, ChessPosition from, Set<ChessMove> moves) {
        int r = from.getRow();
        int c = from.getColumn();

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                tryAddStepMove(board, from, r + dr, c + dc, moves);
            }
        }

        // Castling intentionally not handled here (requires game state).
    }

    private void addSlidingMoves(ChessBoard board, ChessPosition from, Set<ChessMove> moves, int[][] directions) {
        int r = from.getRow();
        int c = from.getColumn();

        for (int[] d : directions) {
            int dr = d[0];
            int dc = d[1];

            int rr = r + dr;
            int cc = c + dc;

            while (onBoard(rr, cc)) {
                ChessPosition to = new ChessPosition(rr, cc);
                ChessPiece target = board.getPiece(to);

                if (target == null) {
                    moves.add(new ChessMove(from, to, null));
                    rr += dr;
                    cc += dc;
                    continue;
                }

                // occupied
                if (target.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(from, to, null));
                }
                break;
            }
        }
    }

    private void tryAddStepMove(ChessBoard board, ChessPosition from, int toRow, int toCol, Set<ChessMove> moves) {
        if (!onBoard(toRow, toCol)) return;

        ChessPosition to = new ChessPosition(toRow, toCol);
        ChessPiece target = board.getPiece(to);

        if (target == null || target.getTeamColor() != this.pieceColor) {
            moves.add(new ChessMove(from, to, null));
        }
    }

    private void addPromotionMoves(ChessPosition from, ChessPosition to, Set<ChessMove> moves) {
        moves.add(new ChessMove(from, to, PieceType.QUEEN));
        moves.add(new ChessMove(from, to, PieceType.ROOK));
        moves.add(new ChessMove(from, to, PieceType.BISHOP));
        moves.add(new ChessMove(from, to, PieceType.KNIGHT));
    }

    private static boolean onBoard(int row, int col) {
        return row >= MIN && row <= MAX && col >= MIN && col <= MAX;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ChessPiece other)
                && this.pieceColor == other.pieceColor
                && this.type == other.type;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + pieceColor.hashCode();
        hash = 31 * hash + type.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        char base = TYPE_TO_CHAR.get(type);
        return String.format(
                "%c",
                pieceColor == ChessGame.TeamColor.BLACK
                        ? Character.toLowerCase(base)
                        : Character.toUpperCase(base)
        );
    }
}
