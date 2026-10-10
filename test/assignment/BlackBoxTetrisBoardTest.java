package assignment;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import org.junit.jupiter.api.Test;

import assignment.Board.Action;
import assignment.Board.Result;
import assignment.Piece.PieceType;

/*
 * Black-box tests for TetrisBoard. These only use the public Board and Piece interfaces
 * (plus the TetrisBoard and TetrisPiece constructors), so they should work for any correct implementation.
 */

public class BlackBoxTetrisBoardTest {

    // makes a new piece of the given type in its spawn rotation
    private Piece piece(PieceType type) {
        return new TetrisPiece(type);
    }

    // makes a new piece of the given type, rotated clockwise the given number of times
    private Piece rotatedPiece(PieceType type, int clockwiseTurns) {
        Piece rotated = new TetrisPiece(type);
        for (int i = 0; i < clockwiseTurns; i++) {
            rotated = rotated.clockwisePiece();
        }
        return rotated;
    }

    // checks that an invalid nextPiece call returns without changing the board
    private void assertNextPieceRejected(Board board, Piece p, Point position) {
        Piece previousPiece = board.getCurrentPiece();
        Point previousPosition = board.getCurrentPiecePosition();
        PieceType[][] previousGrid = new PieceType[board.getWidth()][board.getHeight()];
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                previousGrid[x][y] = board.getGrid(x, y);
            }
        }

        // if this throws an exception, the test fails
        board.nextPiece(p, position);
        assertEquals(previousPiece, board.getCurrentPiece());
        assertEquals(previousPosition, board.getCurrentPiecePosition());
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                assertEquals(previousGrid[x][y], board.getGrid(x, y));
            }
        }
    }

    @Test
    void newBoardIsEmpty() {
        Board board = new TetrisBoard(10, 24);

        // a new board should have the right size, no current piece, and nothing placed anywhere
        assertEquals(10, board.getWidth());
        assertEquals(24, board.getHeight());
        assertEquals(0, board.getMaxHeight());
        assertNull(board.getCurrentPiece());
        assertNull(board.getCurrentPiecePosition());
        for (int x = 0; x < 10; x++) {
            assertEquals(0, board.getColumnHeight(x));
            for (int y = 0; y < 24; y++) {
                assertNull(board.getGrid(x, y));
            }
        }
        for (int y = 0; y < 24; y++) {
            assertEquals(0, board.getRowWidth(y));
        }
    }

    @Test
    void moveWithNoPieceReturnsNoPiece() {
        Board board = new TetrisBoard(10, 24);

        // nothing has been given to the board yet, but the action should still be recorded
        assertEquals(Result.NO_PIECE, board.move(Action.LEFT));
        assertEquals(Result.NO_PIECE, board.getLastResult());
        assertEquals(Action.LEFT, board.getLastAction());
    }

    @Test
    void nextPieceSetsCurrentPieceButNotGrid() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.T), new Point(3, 10));

        assertEquals(PieceType.T, board.getCurrentPiece().getType());
        assertEquals(new Point(3, 10), board.getCurrentPiecePosition());
        // the falling piece should not show up in the grid or the stats
        assertNull(board.getGrid(4, 11));
        assertEquals(0, board.getMaxHeight());
        assertEquals(0, board.getRowWidth(11));
    }

    @Test
    void nextPieceRejectsInvalidInputsWithoutChangingBoard() {
        Board board = new TetrisBoard(10, 24);

        // off the left side and off the top
        assertNextPieceRejected(board, piece(PieceType.T), new Point(-1, 10));
        assertNextPieceRejected(board, piece(PieceType.T), new Point(3, 23));

        // overlapping a placed square in the bottom left corner
        board.nextPiece(piece(PieceType.SQUARE), new Point(0, 10));
        board.move(Action.DROP);
        assertNextPieceRejected(board, piece(PieceType.SQUARE), new Point(0, 0));

        // rejected inputs should also preserve a piece that is already falling
        board.nextPiece(piece(PieceType.T), new Point(4, 10));
        assertNextPieceRejected(board, piece(PieceType.SQUARE), new Point(0, 0));
        assertNextPieceRejected(board, null, new Point(4, 10));
        assertNextPieceRejected(board, piece(PieceType.T), null);
    }

    @Test
    void moveLeftAndRightUntilWall() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.SQUARE), new Point(1, 10));

        assertEquals(Result.SUCCESS, board.move(Action.LEFT));
        assertEquals(new Point(0, 10), board.getCurrentPiecePosition());
        // already against the left wall, so nothing should move
        assertEquals(Result.OUT_BOUNDS, board.move(Action.LEFT));
        assertEquals(new Point(0, 10), board.getCurrentPiecePosition());

        assertEquals(Result.SUCCESS, board.move(Action.RIGHT));
        assertEquals(new Point(1, 10), board.getCurrentPiecePosition());
    }

    @Test
    void moveDownPlacesPieceWhenBlocked() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.SQUARE), new Point(0, 1));

        assertEquals(Result.SUCCESS, board.move(Action.DOWN));
        assertEquals(new Point(0, 0), board.getCurrentPiecePosition());
        // sitting on the floor, so the next DOWN places it
        assertEquals(Result.PLACE, board.move(Action.DOWN));
        assertNull(board.getCurrentPiece());
        assertEquals(PieceType.SQUARE, board.getGrid(0, 0));
        assertEquals(PieceType.SQUARE, board.getGrid(1, 1));
        assertEquals(Result.NO_PIECE, board.move(Action.DOWN));
    }

    @Test
    void dropPlacesPieceAndUpdatesStats() {
        Board board = new TetrisBoard(10, 24);
        // T spawn body: three blocks in a row with one on top of the middle
        board.nextPiece(piece(PieceType.T), new Point(0, 15));

        assertEquals(Result.PLACE, board.move(Action.DROP));
        assertEquals(Action.DROP, board.getLastAction());
        assertNull(board.getCurrentPiece());
        assertEquals(1, board.getColumnHeight(0));
        assertEquals(2, board.getColumnHeight(1));
        assertEquals(1, board.getColumnHeight(2));
        assertEquals(2, board.getMaxHeight());
        assertEquals(3, board.getRowWidth(0));
        assertEquals(1, board.getRowWidth(1));
        assertEquals(PieceType.T, board.getGrid(1, 1));
    }

    @Test
    void dropLandsUnderOverhang() {
        Board board = new TetrisBoard(10, 24);
        // vertical stick fills column 0, rows 0-3
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(-2, 10));
        board.move(Action.DROP);
        // horizontal stick rests on top of it, making a ledge over columns 1-3 at row 4
        board.nextPiece(piece(PieceType.STICK), new Point(0, 10));
        board.move(Action.DROP);
        assertEquals(PieceType.STICK, board.getGrid(3, 4));
        assertNull(board.getGrid(3, 0));

        // a square starting under the ledge should fall to the floor, not jump on top of the ledge
        board.nextPiece(piece(PieceType.SQUARE), new Point(2, 2));
        assertEquals(Result.PLACE, board.move(Action.DROP));
        assertEquals(PieceType.SQUARE, board.getGrid(2, 0));
        assertEquals(PieceType.SQUARE, board.getGrid(3, 1));
        assertNull(board.getGrid(2, 2));
        assertNull(board.getGrid(2, 5));
    }

    @Test
    void dropHeightOnEmptyAndStackedBoard() {
        Board board = new TetrisBoard(10, 24);
        // the T's spawn body starts at y = 1 of its bounding box, so it lands with its origin at -1
        assertEquals(-1, board.dropHeight(piece(PieceType.T), 0));
        assertEquals(0, board.dropHeight(piece(PieceType.SQUARE), 0));

        board.nextPiece(piece(PieceType.SQUARE), new Point(0, 10));
        board.move(Action.DROP);
        // a square on top of a square lands at y = 2
        assertEquals(2, board.dropHeight(piece(PieceType.SQUARE), 0));
    }

    @Test
    void fullRowsAreCleared() {
        Board board = new TetrisBoard(4, 10);
        board.nextPiece(piece(PieceType.SQUARE), new Point(0, 5));
        board.move(Action.DROP);
        assertEquals(0, board.getRowsCleared());

        // the second square fills rows 0 and 1
        board.nextPiece(piece(PieceType.SQUARE), new Point(2, 5));
        board.move(Action.DROP);
        assertEquals(2, board.getRowsCleared());
        assertEquals(0, board.getMaxHeight());
        assertEquals(0, board.getRowWidth(0));
        assertNull(board.getGrid(0, 0));

        // rows cleared only counts the last action
        board.nextPiece(piece(PieceType.SQUARE), new Point(0, 5));
        board.move(Action.DOWN);
        assertEquals(0, board.getRowsCleared());
    }

    @Test
    void blocksAboveClearedRowShiftDown() {
        Board board = new TetrisBoard(3, 10);
        // right L spawn body fills a row of 3 and has one block above the right end
        board.nextPiece(piece(PieceType.RIGHT_L), new Point(0, 5));
        board.move(Action.DROP);

        assertEquals(1, board.getRowsCleared());
        assertEquals(PieceType.RIGHT_L, board.getGrid(2, 0));
        assertNull(board.getGrid(2, 1));
        assertEquals(1, board.getRowWidth(0));
        assertEquals(1, board.getMaxHeight());
    }

    @Test
    void nonAdjacentRowsClearAndRowsBetweenShiftDown() {
        Board board = new TetrisBoard(4, 10);
        // T fills (0,0), (1,0), (2,0) and (1,1)
        board.nextPiece(piece(PieceType.T), new Point(0, 5));
        board.move(Action.DROP);
        // upside down left L fills (0,2), (1,2), (2,2) and (2,1), leaving a hole at (0,1)
        board.nextPiece(rotatedPiece(PieceType.LEFT_L, 2), new Point(0, 6));
        board.move(Action.DROP);
        assertEquals(0, board.getRowsCleared());
        assertNull(board.getGrid(0, 1));

        // vertical stick in column 3 fills rows 0-3, completing rows 0 and 2 but not row 1 (hole) or row 3
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(1, 5));
        board.move(Action.DROP);
        assertEquals(2, board.getRowsCleared());

        // old row 1 drops by 1 to row 0, old row 3 drops by 2 to row 1
        assertNull(board.getGrid(0, 0));
        assertEquals(PieceType.T, board.getGrid(1, 0));
        assertEquals(PieceType.LEFT_L, board.getGrid(2, 0));
        assertEquals(PieceType.STICK, board.getGrid(3, 0));
        assertEquals(PieceType.STICK, board.getGrid(3, 1));
        assertEquals(3, board.getRowWidth(0));
        assertEquals(1, board.getRowWidth(1));
        assertEquals(0, board.getRowWidth(2));
        assertEquals(2, board.getMaxHeight());
    }

    @Test
    void fourRowsClearAtOnce() {
        Board board = new TetrisBoard(2, 10);
        // vertical stick in column 0 fills rows 0-3, but no row is full yet since column 1 is empty
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(-2, 5));
        board.move(Action.DROP);
        assertEquals(0, board.getRowsCleared());

        // a second vertical stick in column 1 completes all 4 rows at once
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(-1, 5));
        board.move(Action.DROP);
        assertEquals(4, board.getRowsCleared());
        assertEquals(0, board.getMaxHeight());
    }

    @Test
    void rotationChangesRotationIndex() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.T), new Point(3, 10));

        // rotate once clockwise, then twice counterclockwise, which should wrap around from 0 to 3
        assertEquals(Result.SUCCESS, board.move(Action.CLOCKWISE));
        assertEquals(1, board.getCurrentPiece().getRotationIndex());
        assertEquals(Result.SUCCESS, board.move(Action.COUNTERCLOCKWISE));
        assertEquals(Result.SUCCESS, board.move(Action.COUNTERCLOCKWISE));
        assertEquals(3, board.getCurrentPiece().getRotationIndex());
        // open space, so no wall kick should have moved it
        assertEquals(new Point(3, 10), board.getCurrentPiecePosition());
    }

    @Test
    void squareRotatesInPlace() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.SQUARE), new Point(4, 10));

        // the square looks the same in every rotation, so it should never move when rotated
        for (int i = 0; i < 4; i++) {
            assertEquals(Result.SUCCESS, board.move(Action.CLOCKWISE));
            assertEquals(new Point(4, 10), board.getCurrentPiecePosition());
        }
    }

    @Test
    void rotationUsesWallKick() {
        Board board = new TetrisBoard(10, 24);
        // T in rotation 1 has no blocks in the left column of its box, so it can sit at x = -1
        board.nextPiece(rotatedPiece(PieceType.T, 1), new Point(-1, 10));

        // rotating again would poke out the left wall, so SRS kicks it right by 1 (R -> 2 offset (+1, 0))
        assertEquals(Result.SUCCESS, board.move(Action.CLOCKWISE));
        assertEquals(2, board.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(0, 10), board.getCurrentPiecePosition());
    }

    @Test
    void movementBlockedByPlacedPiece() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.SQUARE), new Point(0, 10));
        board.move(Action.DROP);

        // a second square right next to the placed one cannot slide into it
        board.nextPiece(piece(PieceType.SQUARE), new Point(2, 0));
        assertEquals(Result.OUT_BOUNDS, board.move(Action.LEFT));
        assertEquals(new Point(2, 0), board.getCurrentPiecePosition());
    }

    @Test
    void rotationKicksOffPlacedPiece() {
        Board board = new TetrisBoard(10, 24);
        // vertical stick fills column 0, rows 0-3
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(-2, 10));
        board.move(Action.DROP);

        // T in rotation 1 sits right next to the stick; rotating in place would overlap the stick at (0,2)
        board.nextPiece(rotatedPiece(PieceType.T, 1), new Point(0, 1));
        assertEquals(Result.SUCCESS, board.move(Action.CLOCKWISE));
        // so SRS kicks it right by 1 (R -> 2 offset (+1, 0))
        assertEquals(2, board.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(1, 1), board.getCurrentPiecePosition());
    }

    @Test
    void stickUsesItsOwnWallKicks() {
        Board board = new TetrisBoard(10, 24);
        // vertical stick in rotation 1 only uses column 2 of its box, so at x = 7 it sits against the right wall
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(7, 10));

        // turning back to horizontal needs 4 columns, so the first two stick kicks (0, 0) and (+2, 0) go off the
        // right wall and the third one (-1, 0) is the first that fits (R -> 0 counterclockwise stick table)
        assertEquals(Result.SUCCESS, board.move(Action.COUNTERCLOCKWISE));
        assertEquals(0, board.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(6, 10), board.getCurrentPiecePosition());
    }

    @Test
    void rotationFailsWhenNoKickFits() {
        Board board = new TetrisBoard(1, 10);
        // a vertical stick in a board 1 column wide can never turn horizontal
        board.nextPiece(rotatedPiece(PieceType.STICK, 1), new Point(-2, 3));

        assertEquals(Result.OUT_BOUNDS, board.move(Action.CLOCKWISE));
        assertEquals(1, board.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(-2, 3), board.getCurrentPiecePosition());
    }

    @Test
    void testMoveDoesNotChangeOriginalBoard() {
        Board board = new TetrisBoard(10, 24);
        board.nextPiece(piece(PieceType.T), new Point(3, 10));

        Board after = board.testMove(Action.DROP);
        assertEquals(Result.PLACE, after.getLastResult());
        assertEquals(PieceType.T, after.getGrid(4, 0));

        // the original still has its falling piece and an empty grid
        assertEquals(new Point(3, 10), board.getCurrentPiecePosition());
        assertNull(board.getGrid(4, 0));
        assertEquals(0, board.getMaxHeight());
    }

    @Test
    void testMoveWithNoPiece() {
        Board board = new TetrisBoard(10, 24);

        // with no current piece, the copy should report NO_PIECE and still match the original
        Board after = board.testMove(Action.DOWN);
        assertEquals(Result.NO_PIECE, after.getLastResult());
        assertEquals(board, after);
    }

    @Test
    void equalsComparesPieceAndGridOnly() {
        Board a = new TetrisBoard(10, 24);
        Board b = new TetrisBoard(10, 24);
        assertEquals(a, b);

        a.nextPiece(piece(PieceType.T), new Point(3, 10));
        assertNotEquals(a, b);
        b.nextPiece(piece(PieceType.T), new Point(3, 10));
        // different last actions should not matter
        a.move(Action.NOTHING);
        assertEquals(a, b);

        // same piece in a different position is not equal
        b.move(Action.LEFT);
        assertNotEquals(a, b);
        assertNotEquals(new TetrisBoard(10, 24), new TetrisBoard(10, 20));
    }
}
