package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 *
 * <p>Note: You can add to this class, but you may not alter signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor teamTurn;

    // The team that forfeited the game. Null if no team has forfeited.
    private TeamColor resigned;

    public ChessGame() {
        this.teamTurn = TeamColor.WHITE;
        this.board = new ChessBoard();
        this.board.resetBoard();
    }

    /**
     * Creates a shallow clone of the given chessboard
     *
     * @param board the board to clone
     * @param teamTurn the team whose turn it is
     */
    public ChessGame(ChessBoard board, TeamColor teamTurn) {
        this.board = new ChessBoard(board);
        this.teamTurn = teamTurn;
    }

    public TeamColor getResigned() {
        return resigned;
    }

    public void setResigned(TeamColor resigned) {
        this.resigned = resigned;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /** Enum identifying the 2 possible teams in a chess game */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChessGame)) return false;
        ChessGame that = (ChessGame) other;

        // Match behavior: compare board + teamTurn only (ignores resigned)
        return Objects.equals(this.board, that.board) && this.teamTurn == that.teamTurn;
    }

    @Override
    public int hashCode() {
        // Must match equals(): use board + teamTurn only
        return Objects.hash(board, teamTurn);
    }

    /**
     * Gets valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) return null;

        Collection<ChessMove> allMoves = piece.pieceMoves(board, startPosition);

        HashSet<ChessMove> safeMoves = new HashSet<>();
        for (ChessMove move : allMoves) {
            ChessGame simulation = new ChessGame(board, teamTurn);
            simulation.getBoard().movePiece(move);

            if (!simulation.isInCheck(piece.getTeamColor())) {
                safeMoves.add(move);
            }
        }
        return safeMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (resigned != null) {
            throw new InvalidMoveException("The game has been resigned. No moves can be made.");
        }
        if (move == null) {
            throw new InvalidMoveException("Move cannot be null.");
        }

        ChessPosition start = move.getStartPosition();
        ChessPiece movingPiece = board.getPiece(start);

        if (movingPiece == null) {
            throw new InvalidMoveException("No piece at the start position.");
        }

        if (movingPiece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("It is not your turn.");
        }

        Collection<ChessMove> legal = validMoves(start);
        if (legal == null || !legal.contains(move)) {
            throw new InvalidMoveException("This piece cannot make this move.");
        }

        board.movePiece(move);
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * <p>Returns true if the specified team’s King could be captured by an opposing piece.
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPiece king = new ChessPiece(teamColor, ChessPiece.PieceType.KING);
        ChessPosition kingPosition = board.findPiece(king);

        if (kingPosition == null) {
            return false;
        }

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece == null) continue;
                if (piece.getTeamColor() == teamColor) continue;

                for (ChessMove move : piece.pieceMoves(board, position)) {
                    if (kingPosition.equals(move.getEndPosition())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * <p>Returns true if the given team has no way to protect their king from being captured.
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) return false;

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece == null) continue;
                if (piece.getTeamColor() != teamColor) continue;

                Collection<ChessMove> moves = validMoves(position);
                if (moves != null && !moves.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate:
     * - It must be that team's turn
     * - The team must NOT be in check
     * - The team has no legal moves
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (teamTurn != teamColor) return false;
        if (isInCheck(teamColor)) return false;

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece == null) continue;
                if (piece.getTeamColor() != teamColor) continue;

                Collection<ChessMove> moves = validMoves(position);
                if (moves != null && !moves.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return this.board;
    }
}
