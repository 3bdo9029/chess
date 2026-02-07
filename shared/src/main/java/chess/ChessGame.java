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

    // =========================
    // Extra credit state
    // =========================

    // If non-null, a pawn may capture onto this square en passant on the immediate next move.
    private ChessPosition enPassantTarget;

    // Track castling rights (whether king/rooks have moved or been captured from home squares)
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

        // For simulations created from just a board + turn, default extra-credit state to "none"
        this.enPassantTarget = null;
        this.whiteKingMoved = this.whiteKingRookMoved = this.whiteQueenRookMoved = false;
        this.blackKingMoved = this.blackKingRookMoved = this.blackQueenRookMoved = false;
    }

    // Private full-state copy ctor (used for simulations so castling/en-passant sim correctly)
    private ChessGame(ChessGame other) {
        this.board = new ChessBoard(other.board);
        this.teamTurn = other.teamTurn;
        this.resigned = other.resigned;

        this.enPassantTarget = other.enPassantTarget;
        this.whiteKingMoved = other.whiteKingMoved;
        this.whiteKingRookMoved = other.whiteKingRookMoved;
        this.whiteQueenRookMoved = other.whiteQueenRookMoved;

        this.blackKingMoved = other.blackKingMoved;
        this.blackKingRookMoved = other.blackKingRookMoved;
        this.blackQueenRookMoved = other.blackQueenRookMoved;
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

        // Match your earlier behavior: compare board + teamTurn only
        return Objects.equals(this.board, that.board) && this.teamTurn == that.teamTurn;
    }

    @Override
    public int hashCode() {
        // Must match equals(): use board + teamTurn only
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
        if (piece == null) return null;

        // Start with all normal moves produced by the piece
        Collection<ChessMove> baseMoves = piece.pieceMoves(board, startPosition);
        HashSet<ChessMove> candidates = new HashSet<>(baseMoves);

        // Extra credit: add castling and en passant candidates (then filter for king safety)
        addCastlingCandidates(piece, startPosition, candidates);
        addEnPassantCandidates(piece, startPosition, candidates);

        // Filter: keep only moves that don't leave own king in check
        HashSet<ChessMove> safeMoves = new HashSet<>();
        for (ChessMove move : candidates) {
            ChessGame sim = new ChessGame(this);
            sim.forceApplyMove(move); // apply with special rules so check eval is accurate
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

        // Apply move (including castling/en passant) and update state
        forceApplyMove(move);

        // Switch turn
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
        // REQUIRED by tests: stalemate only applies to side-to-move
        if (teamTurn != teamColor) return false;

        // REQUIRED by tests: stalemate cannot be while in check
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

    // =========================================================
    // Extra credit implementation: Castling + En Passant
    // =========================================================

    private void addCastlingCandidates(ChessPiece piece, ChessPosition start, HashSet<ChessMove> out) {
        if (piece.getPieceType() != ChessPiece.PieceType.KING) return;

        TeamColor color = piece.getTeamColor();
        int homeRow = (color == TeamColor.WHITE) ? 1 : 8;

        // Only consider from the normal starting king square
        if (start.getRow() != homeRow || start.getColumn() != 5) return;

        // King must not be in check at start
        if (isInCheck(color)) return;

        // King must not have moved
        if (hasKingMoved(color)) return;

        // Kingside castling: e1->g1 or e8->g8
        if (!hasKingRookMoved(color)
                && rookPresent(color, homeRow, 8)
                && board.getPiece(new ChessPosition(homeRow, 6)) == null
                && board.getPiece(new ChessPosition(homeRow, 7)) == null
                && kingStepSafe(color, start, new ChessPosition(homeRow, 6))
                && kingStepSafe(color, start, new ChessPosition(homeRow, 7))) {
            out.add(new ChessMove(start, new ChessPosition(homeRow, 7), null));
        }

        // Queenside castling: e1->c1 or e8->c8
        if (!hasQueenRookMoved(color)
                && rookPresent(color, homeRow, 1)
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
        if (piece.getPieceType() != ChessPiece.PieceType.PAWN) return;
        if (enPassantTarget == null) return;

        int dir = (piece.getTeamColor() == TeamColor.WHITE) ? 1 : -1;

        // Must move one row forward diagonally onto enPassantTarget
        if (enPassantTarget.getRow() != start.getRow() + dir) return;
        if (Math.abs(enPassantTarget.getColumn() - start.getColumn()) != 1) return;

        // Destination must be empty (en passant captures "in passing")
        if (board.getPiece(enPassantTarget) != null) return;

        // Captured pawn sits adjacent on start row at enPassantTarget column
        ChessPosition capturedPos = new ChessPosition(start.getRow(), enPassantTarget.getColumn());
        ChessPiece victim = board.getPiece(capturedPos);

        if (victim != null
                && victim.getPieceType() == ChessPiece.PieceType.PAWN
                && victim.getTeamColor() != piece.getTeamColor()) {
            out.add(new ChessMove(start, enPassantTarget, null));
        }
    }

    /**
     * Applies a move to THIS game without legality checks.
     * Used both by makeMove (after validation) and by simulations in validMoves.
     */
    private void forceApplyMove(ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessPiece moving = board.getPiece(start);
        if (moving == null) return;

        // Track what was on the destination before move (for revoking castling rights if a rook is captured)
        ChessPiece capturedAtEnd = board.getPiece(end);

        // Default: enPassant is only available immediately after a double pawn move
        ChessPosition nextEnPassantTarget = null;

        // --- En passant capture (pawn moves diagonally to empty enPassantTarget) ---
        boolean isEnPassantCapture =
                moving.getPieceType() == ChessPiece.PieceType.PAWN
                        && enPassantTarget != null
                        && end.equals(enPassantTarget)
                        && board.getPiece(end) == null
                        && Math.abs(end.getColumn() - start.getColumn()) == 1;

        if (isEnPassantCapture) {
            ChessPosition capturedPawnPos = new ChessPosition(start.getRow(), end.getColumn());
            board.addPiece(capturedPawnPos, null);
        }

        // --- Castling (king moves 2 columns on same row) ---
        boolean isCastling =
                moving.getPieceType() == ChessPiece.PieceType.KING
                        && start.getRow() == end.getRow()
                        && Math.abs(end.getColumn() - start.getColumn()) == 2;

        if (isCastling) {
            int row = start.getRow();

            // Move king
            board.addPiece(end, moving);
            board.addPiece(start, null);

            // Move rook
            if (end.getColumn() == 7) {
                // Kingside rook: h-file -> f-file
                ChessPosition rookStart = new ChessPosition(row, 8);
                ChessPosition rookEnd = new ChessPosition(row, 6);
                ChessPiece rook = board.getPiece(rookStart);
                board.addPiece(rookEnd, rook);
                board.addPiece(rookStart, null);
            } else {
                // Queenside rook: a-file -> d-file
                ChessPosition rookStart = new ChessPosition(row, 1);
                ChessPosition rookEnd = new ChessPosition(row, 4);
                ChessPiece rook = board.getPiece(rookStart);
                board.addPiece(rookEnd, rook);
                board.addPiece(rookStart, null);
            }
        } else {
            // Normal move (includes promotion via ChessBoard.movePiece)
            revokeCastlingIfRookCaptured(capturedAtEnd, end);
            board.movePiece(move);
        }

        // Update castling rights from the moving piece
        updateCastlingRightsFromMove(moving, start, end);

        // If pawn double-moved, set enPassantTarget for the opponent's immediate next move
        if (moving.getPieceType() == ChessPiece.PieceType.PAWN
                && start.getColumn() == end.getColumn()
                && Math.abs(end.getRow() - start.getRow()) == 2) {
            int midRow = (start.getRow() + end.getRow()) / 2;
            nextEnPassantTarget = new ChessPosition(midRow, start.getColumn());
        }

        enPassantTarget = nextEnPassantTarget;
    }

    private void updateCastlingRightsFromMove(ChessPiece moving, ChessPosition start, ChessPosition end) {
        TeamColor color = moving.getTeamColor();

        if (moving.getPieceType() == ChessPiece.PieceType.KING) {
            if (color == TeamColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }

            // If the king castled, also mark the involved rook as "moved"
            if (start.getRow() == end.getRow() && Math.abs(end.getColumn() - start.getColumn()) == 2) {
                if (color == TeamColor.WHITE) {
                    if (end.getColumn() == 7) whiteKingRookMoved = true;
                    else if (end.getColumn() == 3) whiteQueenRookMoved = true;
                } else {
                    if (end.getColumn() == 7) blackKingRookMoved = true;
                    else if (end.getColumn() == 3) blackQueenRookMoved = true;
                }
            }
        }

        if (moving.getPieceType() == ChessPiece.PieceType.ROOK) {
            if (color == TeamColor.WHITE && start.getRow() == 1) {
                if (start.getColumn() == 1) whiteQueenRookMoved = true;
                if (start.getColumn() == 8) whiteKingRookMoved = true;
            }
            if (color == TeamColor.BLACK && start.getRow() == 8) {
                if (start.getColumn() == 1) blackQueenRookMoved = true;
                if (start.getColumn() == 8) blackKingRookMoved = true;
            }
        }
    }

    private void revokeCastlingIfRookCaptured(ChessPiece captured, ChessPosition square) {
        if (captured == null) return;
        if (captured.getPieceType() != ChessPiece.PieceType.ROOK) return;

        TeamColor color = captured.getTeamColor();
        if (color == TeamColor.WHITE && square.getRow() == 1) {
            if (square.getColumn() == 1) whiteQueenRookMoved = true;
            if (square.getColumn() == 8) whiteKingRookMoved = true;
        }
        if (color == TeamColor.BLACK && square.getRow() == 8) {
            if (square.getColumn() == 1) blackQueenRookMoved = true;
            if (square.getColumn() == 8) blackKingRookMoved = true;
        }
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
