package assignment;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

/** Behavioral tests use only public board operations, not the brain's score formula. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class TetrisBrainTest {
    @Test
    void noActivePieceNeedsNoAction() {
        Board board = new TetrisBoard(10, 24);
        Brain brain = new TetrisBrain();
        Board.Action action = brain.nextMove(board);
        assertEquals(Board.Action.NOTHING, action);
    }

    @Test
    void decisionDoesNotChangeInputBoard() {
        Board board = new TetrisBoard(10, 24);
        Piece activePiece = new TetrisPiece(Piece.PieceType.T);
        Point startingPosition = new Point(3, 20);
        board.nextPiece(activePiece, startingPosition);
        board.move(Board.Action.LEFT);
        Board snapshot = board.testMove(Board.Action.NOTHING);
        Board.Action previousAction = board.getLastAction();
        Board.Result previousResult = board.getLastResult();

        Brain brain = new TetrisBrain();
        Board.Action action = brain.nextMove(board);
        assertNotNull(action);
        assertEquals(snapshot, board);
        assertEquals(previousAction, board.getLastAction());
        assertEquals(previousResult, board.getLastResult());
    }

    @Test
    void rotatesStickToClearNarrowBoard() {
        Board board = new TetrisBoard(4, 8);
        Piece stick = new TetrisPiece(Piece.PieceType.STICK);
        stick = stick.clockwisePiece();
        Point startingPosition = new Point(0, 4);
        board.nextPiece(stick, startingPosition);
        Brain brain = new TetrisBrain();
        for (int step = 0; step < 30; step++) {
            if (board.getCurrentPiece() == null) {
                break;
            }
            Board.Action action = brain.nextMove(board);
            board.move(action);
        }
        assertNull(board.getCurrentPiece(), "Brain should finish placing the piece");
        assertEquals(1, board.getRowsCleared());
        assertEquals(0, board.getMaxHeight());
    }

    @Test
    void survivesOneHundredPiecesOnAverageAcrossFiveRuns() {
        int totalPlaced = 0;
        for (int seed = 0; seed < 5; seed++) {
            int placed = play(seed, 150);
            System.out.println("Brain seed " + seed + ": " + placed + " pieces (cap 150)");
            totalPlaced = totalPlaced + placed;
        }
        double average = totalPlaced / 5.0;
        assertTrue(average >= 100,
                "Expected average of at least 100 pieces, got " + average);
    }

    @Test
    void emptyBoardDecisionPreservesMetadata() {
        Board board = new TetrisBoard(10, 24);
        board.move(Board.Action.LEFT);
        Brain brain = new TetrisBrain();
        assertDecisionPreservesBoard(brain, board);
    }

    @ParameterizedTest
    @EnumSource(Piece.PieceType.class)
    void handlesEveryPieceRotationAtWallsAndDifferentHeights(Piece.PieceType type) {
        Brain brain = new TetrisBrain();
        for (int rotation = 0; rotation < 4; rotation++) {
            Piece piece = rotated(type, rotation);
            int minX = minimumX(piece);
            int maxX = maximumX(piece);
            int minY = minimumY(piece);
            int leftOrigin = -minX;
            int rightOrigin = 9 - maxX;
            int floorOrigin = -minY;
            int[] origins = {leftOrigin, 3, rightOrigin};
            int[] heights = {floorOrigin, 5, 20};
            for (int origin : origins) {
                for (int height : heights) {
                    Board board = new TetrisBoard(10, 24);
                    Point startingPosition = new Point(origin, height);
                    board.nextPiece(piece, startingPosition);
                    assertNotNull(board.getCurrentPiece(), "Fixture must contain an active piece");
                    assertDecisionPreservesBoard(brain, board);
                    placeWithBrain(brain, board);
                    assertEquals(4, occupiedCells(board), "One tetromino contributes four cells");
                    // S and Z pieces necessarily leave one hole when placed alone
                    // on a flat floor, regardless of their orientation.
                    int expectedHoles = 0;
                    if (type == Piece.PieceType.LEFT_DOG) {
                        expectedHoles = 1;
                    } else if (type == Piece.PieceType.RIGHT_DOG) {
                        expectedHoles = 1;
                    }
                    assertEquals(expectedHoles, countHoles(board),
                            "Avoid unnecessary holes: " + type + ", rotation " + rotation
                                    + ", origin (" + origin + ", " + height + ")");
                    Board.Action actionAfterPlacement = brain.nextMove(board);
                    assertEquals(Board.Action.NOTHING, actionAfterPlacement);
                }
            }
        }
    }

    @Test
    void movesLeftToFillGapAndClearTwoRows() {
        assertSquareFillsGap(0);
    }

    @Test
    void movesRightToFillGapAndClearTwoRows() {
        assertSquareFillsGap(8);
    }

    @Test
    void rotatesAndMovesToClearFourRows() {
        Board board = new TetrisBoard(10, 24);
        Piece verticalStick = rotated(Piece.PieceType.STICK, 1);
        for (int x = 0; x < 10; x++) {
            if (x != 8) {
                placeFixture(board, verticalStick, x, 0);
            }
        }
        Piece activePiece = new TetrisPiece(Piece.PieceType.STICK);
        Point startingPosition = new Point(3, 20);
        board.nextPiece(activePiece, startingPosition);
        Brain brain = new TetrisBrain();
        placeWithBrain(brain, board);
        assertEquals(4, board.getRowsCleared());
        assertEquals(0, occupiedCells(board));
    }

    @Test
    void avoidsPlacingAcrossAnEdgeThatWouldCreateHoles() {
        Board board = new TetrisBoard(10, 24);
        Piece squareFixture = new TetrisPiece(Piece.PieceType.SQUARE);
        placeFixture(board, squareFixture, 4, 0);
        Piece activePiece = new TetrisPiece(Piece.PieceType.STICK);
        Point startingPosition = new Point(3, 20);
        board.nextPiece(activePiece, startingPosition);
        Brain brain = new TetrisBrain();
        placeWithBrain(brain, board);
        assertEquals(0, countHoles(board));
        assertEquals(8, occupiedCells(board));
        assertEquals(2, board.getMaxHeight(), "There is room to lay the stick on the floor");
    }

    @Test
    void choosesLowerSideOfUnevenStack() {
        Board board = new TetrisBoard(6, 24);
        Piece square = new TetrisPiece(Piece.PieceType.SQUARE);
        placeFixture(board, square, 0, 0);
        placeFixture(board, square, 0, 2);
        placeFixture(board, square, 2, 0);
        Point startingPosition = new Point(1, 20);
        board.nextPiece(square, startingPosition);
        Brain brain = new TetrisBrain();
        placeWithBrain(brain, board);
        assertEquals(2, board.getRowsCleared());
        assertEquals(2, board.getMaxHeight());
        assertEquals(0, countHoles(board));
    }

    @Test
    void finishesEvenWhenEveryAvailablePlacementIsHigh() {
        Board board = new TetrisBoard(4, 40);
        Piece square = new TetrisPiece(Piece.PieceType.SQUARE);
        for (int y = 0; y < 24; y = y + 2) {
            placeFixture(board, square, 0, y);
        }
        Point startingPosition = new Point(0, 30);
        board.nextPiece(square, startingPosition);
        Brain brain = new TetrisBrain();
        placeWithBrain(brain, board);
        assertEquals(2, board.getRowsCleared());
        assertEquals(22, board.getMaxHeight());
    }

    @Test
    void dropsImmediatelyWhenNoLateralMoveOrRotationCanImprovePlacement() {
        Board board = new TetrisBoard(2, 8);
        Piece activePiece = new TetrisPiece(Piece.PieceType.SQUARE);
        Point startingPosition = new Point(0, 5);
        board.nextPiece(activePiece, startingPosition);
        Brain brain = new TetrisBrain();
        Board.Action action = brain.nextMove(board);
        assertEquals(Board.Action.DROP, action);
    }

    @Test
    void preservesNonemptyBoardAndPreviousLineClearMetadata() {
        Board board = new TetrisBoard(4, 12);
        Piece squareFixture = new TetrisPiece(Piece.PieceType.SQUARE);
        placeFixture(board, squareFixture, 0, 0);
        Piece stickFixture = new TetrisPiece(Piece.PieceType.STICK);
        placeFixture(board, stickFixture, 0, 2);
        assertEquals(1, board.getRowsCleared());
        Piece activePiece = new TetrisPiece(Piece.PieceType.T);
        Point startingPosition = new Point(0, 8);
        board.nextPiece(activePiece, startingPosition);
        Brain brain = new TetrisBrain();
        assertDecisionPreservesBoard(brain, board);
    }

    @Test
    void handlesBoardWithExistingBuriedHoles() {
        Board board = new TetrisBoard(10, 24);
        Piece verticalStick = rotated(Piece.PieceType.STICK, 1);
        placeFixture(board, verticalStick, 4, 0);
        Piece squareFixture = new TetrisPiece(Piece.PieceType.SQUARE);
        placeFixture(board, squareFixture, 3, 4);
        assertEquals(4, countHoles(board), "Fixture has an overhang above four empty cells");
        Piece activePiece = new TetrisPiece(Piece.PieceType.RIGHT_L);
        Point startingPosition = new Point(3, 20);
        board.nextPiece(activePiece, startingPosition);
        Brain brain = new TetrisBrain();
        assertDecisionPreservesBoard(brain, board);
        placeWithBrain(brain, board);
    }

    @Test
    void noActivePieceAfterLineClearPreservesClearCount() {
        Board board = new TetrisBoard(4, 8);
        Piece stickFixture = new TetrisPiece(Piece.PieceType.STICK);
        placeFixture(board, stickFixture, 0, 0);
        assertEquals(1, board.getRowsCleared());
        Brain brain = new TetrisBrain();
        assertDecisionPreservesBoard(brain, board);
        Board.Action action = brain.nextMove(board);
        assertEquals(Board.Action.NOTHING, action);
    }

    @Test
    void sameBrainCanHandleDifferentBoardsWithoutLeakingState() {
        Brain shared = new TetrisBrain();
        Board first = new TetrisBoard(10, 24);
        Piece firstPiece = new TetrisPiece(Piece.PieceType.T);
        Point firstPosition = new Point(3, 20);
        first.nextPiece(firstPiece, firstPosition);
        Board.Action expected = shared.nextMove(first);
        Board second = new TetrisBoard(4, 8);
        Piece secondPiece = rotated(Piece.PieceType.STICK, 1);
        Point secondPosition = new Point(0, 4);
        second.nextPiece(secondPiece, secondPosition);
        placeWithBrain(shared, second);
        Board.Action decisionAfterOtherBoard = shared.nextMove(first);
        assertEquals(expected, decisionAfterOtherBoard);

        Brain freshBrain = new TetrisBrain();
        Board.Action freshDecision = freshBrain.nextMove(first);
        Board.Action sharedDecision = shared.nextMove(first);
        assertEquals(freshDecision, sharedDecision);
    }

    @Test
    void survivesBroaderSetOfRandomSequences() {
        int totalPlaced = 0;
        int minimum = Integer.MAX_VALUE;
        for (int run = 0; run < 20; run++) {
            int seed = 1000 + run * 97;
            int placed = play(seed, 200);
            totalPlaced = totalPlaced + placed;
            if (placed < minimum) {
                minimum = placed;
            }
        }
        double average = totalPlaced / 20.0;
        System.out.println("Additional 20 runs: average " + average
                + ", minimum " + minimum + " (cap 200)");
        assertTrue(average >= 100, "Average must reach 100 across additional seeds");
    }

    private void assertSquareFillsGap(int gapX) {
        Board board = new TetrisBoard(10, 24);
        Piece square = new TetrisPiece(Piece.PieceType.SQUARE);
        for (int x = 0; x < 10; x = x + 2) {
            if (x != gapX) {
                placeFixture(board, square, x, 0);
            }
        }
        Point startingPosition = new Point(4, 20);
        board.nextPiece(square, startingPosition);
        Brain brain = new TetrisBrain();
        Board.Action first = brain.nextMove(board);
        if (gapX == 0) {
            assertEquals(Board.Action.LEFT, first);
        } else {
            assertEquals(Board.Action.RIGHT, first);
        }
        placeWithBrain(brain, board);
        assertEquals(2, board.getRowsCleared());
        assertEquals(0, occupiedCells(board));
    }

    // Install fixtures by aligning actual blocks, rather than assuming the SRS origin
    // is the lowest occupied cell. All fixtures are constructed through public methods.
    private void placeFixture(Board board, Piece piece, int left, int bottom) {
        int originX = left - minimumX(piece);
        int originY = bottom - minimumY(piece);
        Point origin = new Point(originX, originY);
        board.nextPiece(piece, origin);
        assertNotNull(board.getCurrentPiece(), "Fixture piece must fit");

        Board.Result result = board.move(Board.Action.DROP);
        assertEquals(Board.Result.PLACE, result);
    }

    private Piece rotated(Piece.PieceType type, int turns) {
        Piece piece = new TetrisPiece(type);
        for (int turn = 0; turn < turns; turn++) {
            piece = piece.clockwisePiece();
        }
        return piece;
    }

    private int minimumX(Piece piece) {
        int result = Integer.MAX_VALUE;
        for (Point block : piece.getBody()) {
            if (block.x < result) {
                result = block.x;
            }
        }
        return result;
    }

    private int maximumX(Piece piece) {
        int result = Integer.MIN_VALUE;
        for (Point block : piece.getBody()) {
            if (block.x > result) {
                result = block.x;
            }
        }
        return result;
    }

    private int minimumY(Piece piece) {
        int result = Integer.MAX_VALUE;
        for (Point block : piece.getBody()) {
            if (block.y < result) {
                result = block.y;
            }
        }
        return result;
    }

    private int occupiedCells(Board board) {
        int count = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                if (board.getGrid(x, y) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countHoles(Board board) {
        int holes = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            boolean blockAbove = false;
            for (int y = board.getHeight() - 1; y >= 0; y--) {
                if (board.getGrid(x, y) != null) {
                    blockAbove = true;
                } else if (blockAbove) {
                    holes++;
                }
            }
        }
        return holes;
    }

    private void assertDecisionPreservesBoard(Brain brain, Board board) {
        Board snapshot = board.testMove(Board.Action.NOTHING);
        Board.Action previousAction = board.getLastAction();
        Board.Result previousResult = board.getLastResult();
        int previousClears = board.getRowsCleared();
        Piece piece = board.getCurrentPiece();
        Point[] body = null;
        int[] skirt = null;
        if (piece != null) {
            Point[] originalBody = piece.getBody();
            body = new Point[originalBody.length];
            for (int i = 0; i < body.length; i++) {
                Point originalBlock = originalBody[i];
                body[i] = new Point(originalBlock);
            }

            int[] originalSkirt = piece.getSkirt();
            skirt = new int[originalSkirt.length];
            for (int i = 0; i < skirt.length; i++) {
                skirt[i] = originalSkirt[i];
            }
        }
        Board.Action decision = brain.nextMove(board);
        assertNotNull(decision);
        assertEquals(snapshot, board);
        assertEquals(previousAction, board.getLastAction());
        assertEquals(previousResult, board.getLastResult());
        assertEquals(previousClears, board.getRowsCleared());
        assertEquals(snapshot.getMaxHeight(), board.getMaxHeight());
        for (int x = 0; x < board.getWidth(); x++) {
            assertEquals(snapshot.getColumnHeight(x), board.getColumnHeight(x));
            for (int y = 0; y < board.getHeight(); y++) {
                assertEquals(snapshot.getGrid(x, y), board.getGrid(x, y));
            }
        }
        for (int y = 0; y < board.getHeight(); y++) {
            assertEquals(snapshot.getRowWidth(y), board.getRowWidth(y));
        }
        if (piece != null) {
            assertArrayEquals(body, piece.getBody());
            assertArrayEquals(skirt, piece.getSkirt());
        }
        Board.Action repeatedDecision = brain.nextMove(board);
        assertEquals(decision, repeatedDecision, "Same position should give same decision");
    }

    // Exercise actual decisions until placement, rather than checking only the first action.
    private void placeWithBrain(Brain brain, Board board) {
        int before = occupiedCells(board);
        for (int step = 0; step < 100; step++) {
            if (board.getCurrentPiece() == null) {
                break;
            }

            Board.Action action = brain.nextMove(board);
            assertNotNull(action);
            assertNotEquals(Board.Action.NOTHING, action, "Active piece must make progress");
            assertNotEquals(Board.Action.HOLD, action);
            Board.Result result = board.move(action);
            boolean validResult = false;
            if (result == Board.Result.SUCCESS) {
                validResult = true;
            } else if (result == Board.Result.PLACE) {
                validResult = true;
            }
            assertTrue(validResult,
                    "Brain returned an invalid action: " + action + " -> " + result);
        }
        assertNull(board.getCurrentPiece(), "Brain must place within 100 actions");
        assertEquals(Board.Result.PLACE, board.getLastResult());
        int cellsAdded = 4;
        int cellsCleared = board.getRowsCleared() * board.getWidth();
        int expectedCells = before + cellsAdded - cellsCleared;
        int actualCells = occupiedCells(board);
        assertEquals(expectedCells, actualCells,
                "Placement and clears must conserve occupied cells");
    }

    private int play(int seed, int limit) {
        Random random = new Random(seed);
        Brain brain = new TetrisBrain();
        Board board = new TetrisBoard(10, 24);
        Piece.PieceType[] types = Piece.PieceType.values();
        int placed = 0;
        while (placed < limit) {
            int typeIndex = random.nextInt(types.length);
            Piece.PieceType type = types[typeIndex];
            Piece piece = new TetrisPiece(type);
            int spawnX = 5 - piece.getWidth() / 2;
            Point spawn = new Point(spawnX, 20);
            for (Point block : piece.getBody()) {
                int blockX = spawn.x + block.x;
                int blockY = spawn.y + block.y;
                Piece.PieceType existingBlock = board.getGrid(blockX, blockY);
                if (existingBlock != null) {
                    return placed;
                }
            }
            board.nextPiece(piece, spawn);
            assertNotNull(board.getCurrentPiece(), "Spawn should succeed after collision check");
            placeWithBrain(brain, board);
            if (board.getMaxHeight() > 20) {
                return placed;
            }
            placed++;
        }
        return placed;
    }
}
