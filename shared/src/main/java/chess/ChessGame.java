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
    private TeamColor resigned;
    private boolean gameOver;
    private ChessPosition enPassantTarget;
    private boolean whiteKingMoved, whiteKingRookMoved, whiteQueenRookMoved;
    private boolean blackKingMoved, blackKingRookMoved, blackQueenRookMoved;

    public ChessGame() {
        this.teamTurn = TeamColor.WHITE;
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.enPassantTarget = null;
        this.whiteKingMoved = this.whiteKingRookMoved = this.whiteQueenRookMoved = false;
        this.blackKingMoved = this.blackKingRookMoved = this.blackQueenRookMoved = false;
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
        this.enPassantTarget = null;
        this.whiteKingMoved = this.whiteKingRookMoved = this.whiteQueenRookMoved = false;
        this.blackKingMoved = this.blackKingRookMoved = this.blackQueenRookMoved = false;
    }

    private ChessGame(ChessGame other) {
        this.board = new ChessBoard(other.board);
        this.teamTurn = other.teamTurn;
        this.resigned = other.resigned;
        this.gameOver = other.gameOver;
        this.enPassantTarget = other.enPassantTarget;
        this.whiteKingMoved = other.whiteKingMoved;
        this.whiteKingRookMoved = other.whiteKingRookMoved;
        this.whiteQueenRookMoved = other.whiteQueenRookMoved;
        this.blackKingMoved = other.blackKingMoved;
        this.blackKingRookMoved = other.blackKingRookMoved;
        this.blackQueenRookMoved = other.blackQueenRookMoved;
    }

    public TeamColor getResigned() { return resigned; }

    public void setResigned(TeamColor resigned) { this.resigned = resigned; }

    public boolean isGameOver() { return gameOver || resigned != null; }

    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    /** @return Which team's turn it is */
    public TeamColor getTeamTurn() { return teamTurn; }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) { teamTurn = team; }

    /** Enum identifying the 2 possible teams in a chess game */
    public enum TeamColor { WHITE, BLACK }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChessGame)) {
            return false;
        }
        ChessGame that = (ChessGame) other;
        return Objects.equals(this.board, that.board) && this.teamTurn == that.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn);
    }

    /**
     * Gets valid moves for a piece at the given location.
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }
        Collection<ChessMove> baseMoves = piece.pieceMoves(board, startPosition);
        HashSet<ChessMove> candidates = new HashSet<>(baseMoves);
        addCastlingCandidates(piece, startPosition, candidates);
        addEnPassantCandidates(piece, startPosition, candidates);
        HashSet<ChessMove> safeMoves = new HashSet<>();
        for (ChessMove move : candidates) {
            ChessGame sim = new ChessGame(this);
            sim.forceApplyMove(move);
            if (!sim.isInCheck(piece.getTeamColor())) {
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
        if (isGameOver()) {
            throw new InvalidMoveException("The game is over. No moves can be made.");
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
        forceApplyMove(move);
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
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
                if (piece == null) {
                    continue;
                }
                if (piece.getTeamColor() == teamColor) {
                    continue;
                }
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
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }
        return !hasAnyLegalMove(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, meaning the team is not in check but has no
     * legal moves.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (teamTurn != teamColor) {
            return false;
        }
        if (isInCheck(teamColor)) {
            return false;
        }
        return !hasAnyLegalMove(teamColor);
    }

    /** Returns true if the given team has at least one legal move available. */
    private boolean hasAnyLegalMove(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece == null) {
                    continue;
                }
                if (piece.getTeamColor() != teamColor) {
                    continue;
                }
                Collection<ChessMove> moves = validMoves(position);
                if (moves != null && !moves.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) { this.board = board; }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() { return this.board; }

    private void addCastlingCandidates(ChessPiece piece, ChessPosition start, HashSet<ChessMove> out) {
        if (piece.getPieceType() != ChessPiece.PieceType.KING) {
            return;
        }
        TeamColor color = piece.getTeamColor();
        int homeRow = (color == TeamColor.WHITE) ? 1 : 8;
        if (start.getRow() != homeRow || start.getColumn() != 5) {
            return;
        }
        if (isInCheck(color)) {
            return;
        }
        if (hasKingMoved(color)) {
            return;
        }
        if (!hasKingRookMoved(color) && rookPresent(color, homeRow, 8)
                && board.getPiece(new ChessPosition(homeRow, 6)) == null
                && board.getPiece(new ChessPosition(homeRow, 7)) == null
                && kingStepSafe(color, start, new ChessPosition(homeRow, 6))
                && kingStepSafe(color, start, new ChessPosition(homeRow, 7))) {
            out.add(new ChessMove(start, new ChessPosition(homeRow, 7), null));
        }
        if (!hasQueenRookMoved(color) && rookPresent(color, homeRow, 1)
                && board.getPiece(new ChessPosition(homeRow, 2)) == null
                && board.getPiece(new ChessPosition(homeRow, 3)) == null
                && board.getPiece(new ChessPosition(homeRow, 4)) == null
                && kingStepSafe(color, start, new ChessPosition(homeRow, 4))
                && kingStepSafe(color, start, new ChessPosition(homeRow, 3))) {
            out.add(new ChessMove(start, new ChessPosition(homeRow, 3), null));
        }
    }

    private boolean kingStepSafe(TeamColor color, ChessPosition from, ChessPosition to) {
        ChessGame sim = new ChessGame(this);
        ChessPiece king = sim.board.getPiece(from);
        sim.board.addPiece(to, king);
        sim.board.addPiece(from, null);
        return !sim.isInCheck(color);
    }

    private boolean rookPresent(TeamColor color, int row, int col) {
        ChessPiece rook = board.getPiece(new ChessPosition(row, col));
        return rook != null
                && rook.getTeamColor() == color
                && rook.getPieceType() == ChessPiece.PieceType.ROOK;
    }

    private void addEnPassantCandidates(ChessPiece piece, ChessPosition start, HashSet<ChessMove> out) {
        if (piece.getPieceType() != ChessPiece.PieceType.PAWN) {
            return;
        }
        if (enPassantTarget == null) {
            return;
        }
        int dir = (piece.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
        if (enPassantTarget.getRow() != start.getRow() + dir) {
            return;
        }
        if (Math.abs(enPassantTarget.getColumn() - start.getColumn()) != 1) {
            return;
        }
        if (board.getPiece(enPassantTarget) != null) {
            return;
        }
        ChessPosition capturedPos = new ChessPosition(start.getRow(), enPassantTarget.getColumn());
        ChessPiece victim = board.getPiece(capturedPos);
        if (victim != null
                && victim.getPieceType() == ChessPiece.PieceType.PAWN
                && victim.getTeamColor() != piece.getTeamColor()) {
            out.add(new ChessMove(start, enPassantTarget, null));
        }
    }

    private void forceApplyMove(ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessPiece moving = board.getPiece(start);
        if (moving == null) {
            return;
        }
        ChessPiece capturedAtEnd = board.getPiece(end);
        ChessPosition nextEnPassantTarget = null;
        boolean isEnPassantCapture =
                moving.getPieceType() == ChessPiece.PieceType.PAWN
                        && enPassantTarget != null
                        && end.equals(enPassantTarget)
                        && board.getPiece(end) == null
                        && Math.abs(end.getColumn() - start.getColumn()) == 1;
        if (isEnPassantCapture) {
            board.addPiece(new ChessPosition(start.getRow(), end.getColumn()), null);
        }
        boolean isCastling =
                moving.getPieceType() == ChessPiece.PieceType.KING
                        && start.getRow() == end.getRow()
                        && Math.abs(end.getColumn() - start.getColumn()) == 2;
        if (isCastling) {
            applyCastling(moving, start, end);
        } else {
            revokeCastlingIfRookCaptured(capturedAtEnd, end);
            board.movePiece(move);
        }
        updateCastlingRightsFromMove(moving, start, end);
        if (moving.getPieceType() == ChessPiece.PieceType.PAWN
                && start.getColumn() == end.getColumn()
                && Math.abs(end.getRow() - start.getRow()) == 2) {
            int midRow = (start.getRow() + end.getRow()) / 2;
            nextEnPassantTarget = new ChessPosition(midRow, start.getColumn());
        }
        enPassantTarget = nextEnPassantTarget;
    }

    private void applyCastling(ChessPiece moving, ChessPosition start, ChessPosition end) {
        int row = start.getRow();
        board.addPiece(end, moving);
        board.addPiece(start, null);
        if (end.getColumn() == 7) {
            ChessPosition rookFrom = new ChessPosition(row, 8);
            ChessPiece rook = board.getPiece(rookFrom);
            board.addPiece(new ChessPosition(row, 6), rook);
            board.addPiece(rookFrom, null);
        } else {
            ChessPosition rookFrom = new ChessPosition(row, 1);
            ChessPiece rook = board.getPiece(rookFrom);
            board.addPiece(new ChessPosition(row, 4), rook);
            board.addPiece(rookFrom, null);
        }
    }

    private void updateCastlingRightsFromMove(ChessPiece moving, ChessPosition start, ChessPosition end) {
        TeamColor color = moving.getTeamColor();
        if (moving.getPieceType() == ChessPiece.PieceType.KING) {
            if (color == TeamColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
            if (start.getRow() == end.getRow() && Math.abs(end.getColumn() - start.getColumn()) == 2) {
                updateKingCastlingRookMoved(color, end.getColumn());
            }
        }
        if (moving.getPieceType() == ChessPiece.PieceType.ROOK) {
            updateRookCastlingRights(color, start);
        }
    }

    private void updateKingCastlingRookMoved(TeamColor color, int endCol) {
        if (color == TeamColor.WHITE) {
            if (endCol == 7) {
                whiteKingRookMoved = true;
            } else if (endCol == 3) {
                whiteQueenRookMoved = true;
            }
        } else {
            if (endCol == 7) {
                blackKingRookMoved = true;
            } else if (endCol == 3) {
                blackQueenRookMoved = true;
            }
        }
    }

    private void updateRookCastlingRights(TeamColor color, ChessPosition start) {
        if (color == TeamColor.WHITE && start.getRow() == 1) {
            if (start.getColumn() == 1) {
                whiteQueenRookMoved = true;
            }
            if (start.getColumn() == 8) {
                whiteKingRookMoved = true;
            }
        }
        if (color == TeamColor.BLACK && start.getRow() == 8) {
            if (start.getColumn() == 1) {
                blackQueenRookMoved = true;
            }
            if (start.getColumn() == 8) {
                blackKingRookMoved = true;
            }
        }
    }

    private void revokeCastlingIfRookCaptured(ChessPiece captured, ChessPosition square) {
        if (captured == null) {
            return;
        }
        if (captured.getPieceType() != ChessPiece.PieceType.ROOK) {
            return;
        }
        updateRookCastlingRights(captured.getTeamColor(), square);
    }

    private boolean hasKingMoved(TeamColor color) {
        return (color == TeamColor.WHITE) ? whiteKingMoved : blackKingMoved;
    }

    private boolean hasKingRookMoved(TeamColor color) {
        return (color == TeamColor.WHITE) ? whiteKingRookMoved : blackKingRookMoved;
    }

    private boolean hasQueenRookMoved(TeamColor color) {
        return (color == TeamColor.WHITE) ? whiteQueenRookMoved : blackQueenRookMoved;
    }
}
