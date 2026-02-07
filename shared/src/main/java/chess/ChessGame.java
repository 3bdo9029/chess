package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
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
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChessGame)) return false;

        ChessGame that = (ChessGame) other;

        // Match original behavior: compares board + teamTurn only (ignores resigned)
        return Objects.equals(this.board, that.board) && this.teamTurn == that.teamTurn;
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

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
        // A move is illegal if the game has been resigned.
        if (resigned != null) {
            throw new InvalidMoveException("The game has been resigned. No moves can be made.");
        }

        ChessPosition start = move.getStartPosition();
        ChessPiece movingPiece = board.getPiece(start);

        // Keep behavior consistent with "invalid move" handling.
        if (movingPiece == null) {
            throw new InvalidMoveException("No piece at the start position.");
        }

        // A move is illegal if it’s not the corresponding team's turn.
        if (movingPiece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("It is not your turn.");
        }

        // A move is illegal if the chess piece cannot move there.
        Collection<ChessMove> legal = validMoves(start);
        if (legal == null || !legal.contains(move)) {
            throw new InvalidMoveException("This piece cannot make this move.");
        }

        // Otherwise, the move is legal, and the piece is moved.
        board.movePiece(move);
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        // Find the position of the king.
        ChessPiece king = new ChessPiece(teamColor, ChessPiece.PieceType.KING);
        ChessPosition kingPosition = board.findPiece(king);

        // If there is no king, we cannot be in check.
        if (kingPosition == null) {
            System.out.println(String.format("No %s found in board:\n%s", king, board));
            return false;
        }

        // For every enemy piece on the board, check if it can move to the same
        // position as the king (i.e. capture the king).
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

        // No enemy piece can capture the king.
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        // If we are not in check, we cannot be in checkmate.
        if (!isInCheck(teamColor)) {
            return false;
        }

        // For every friendly piece on the board, check if it can move to a position
        // where we would no longer be in check.
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece == null) continue;
                if (piece.getTeamColor() != teamColor) continue;

                for (ChessMove move : piece.pieceMoves(board, position)) {
                    ChessGame simulation = new ChessGame(board, teamTurn);
                    simulation.getBoard().movePiece(move);

                    if (!simulation.isInCheck(teamColor)) {
                        return false;
                    }
                }
            }
        }

        // No friendly piece can move to a position where we would no longer be in
        // check. Thus, we are in checkmate and have lost the game.
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        // For every friendly piece on the board, check if it can move to a
        // position. If any friendly piece can move, we are not in stalemate.
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

        // No friendly piece can make a move. Thus, we are in stalemate.
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
