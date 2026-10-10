package assignment;

import java.awt.*;

/**
 * Represents a Tetris board -- essentially a 2-d grid of piece types (or nulls). Supports
 * tetris pieces and row clearing.  Does not do any drawing or have any idea of
 * pixels. Instead, just represents the abstract 2-d board.
 */
public final class TetrisBoard implements Board {

    // define variables that will be used throughout the class
    private final Piece.PieceType[][] grid;
    private final int width;
    private final int height;
    private Piece currentPiece;
    private Point currentPiecePosition;
    private Action lastAction;
    private Result lastResult;
    private int rowsCleared;
    // cached stats so the accessors run in constant time; refreshed whenever the grid changes
    private int[] columnHeights;
    private int[] rowWidths;
    private int maxHeight;

    // JTetris will use this constructor
    public TetrisBoard(int width, int height) {
        // update class variables accordingly
        this.width = width;
        this.height = height;
        this.grid = new Piece.PieceType[width][height];
        this.columnHeights = new int[width];
        this.rowWidths = new int[height];
    }

    // handles piece rotations, drops, and sliding
    @Override
    public Result move(Action act) { 
        // update last action
        lastAction = act;
        rowsCleared = 0;
        if (currentPiece == null || currentPiecePosition == null) {
            lastResult = Result.NO_PIECE;
            return lastResult;
        }

        // calls rotateCurrentPiece method if action is clockwise/counterclockwise
        if (act == Action.CLOCKWISE || act == Action.COUNTERCLOCKWISE) {
            return rotateCurrentPiece(act == Action.CLOCKWISE);
        }

        if (act == Action.DROP) {
            // slide the piece down until it is blocked and place it
            Point landing = new Point(currentPiecePosition);
            while (canPlace(currentPiece, new Point(landing.x, landing.y - 1))) {
                landing.y--;
            }
            currentPiecePosition = landing;
            placeCurrentPiece();
            lastResult = Result.PLACE;
            return lastResult;
        }

        int deltax = 0;
        int deltay = 0;
        if (act == Action.LEFT) {
            // shift piece 1 column left
            deltax = -1;
        } else if (act == Action.RIGHT) {
            // shift piece 1 column right
            deltax = 1;
        } else if (act == Action.DOWN) {
            // shift piece 1 row down
            deltay = -1;
        } else if (act == Action.NOTHING || act == Action.HOLD) {
            // if the action passed in was nothing or hold, return success automatically
            lastResult = Result.SUCCESS;
            return lastResult;
        } else {
            // rotations will be handled separately
            lastResult = Result.OUT_BOUNDS;
            return lastResult;
        }

        // implemented movements on a separate piece variable
        Point proposedPosition = new Point(currentPiecePosition.x + deltax,
                                           currentPiecePosition.y + deltay);
        if (canPlace(currentPiece, proposedPosition)) {
            // if not out of bounds AND doesn't overlap with another piece, replace current falling piece with proposed piece
            currentPiecePosition = proposedPosition;
            lastResult = Result.SUCCESS;
        } else {
            if (act == Action.DOWN) {
                // place the piece when it cannot move down
                placeCurrentPiece();
                lastResult = Result.PLACE;
            } else {
                // if not out of bounds OR doesn't overlap with another piece, DON'T replace current falling piece with proposed piece
                lastResult = Result.OUT_BOUNDS;
            }
        }
        return lastResult;
    }

    @Override
    public Board testMove(Action act) { 
        // make a copy of the board so the action does not change this board
        TetrisBoard boardCopy = new TetrisBoard(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boardCopy.grid[x][y] = grid[x][y];
            }
        }

        boardCopy.columnHeights = columnHeights.clone();
        boardCopy.rowWidths = rowWidths.clone();
        boardCopy.maxHeight = maxHeight;

        // copy the current piece and its position
        boardCopy.currentPiece = currentPiece;
        if (currentPiecePosition != null) {
            boardCopy.currentPiecePosition = new Point(currentPiecePosition);
        }

        // copy the previous action state before applying the new action
        boardCopy.lastAction = lastAction;
        boardCopy.lastResult = lastResult;
        boardCopy.rowsCleared = rowsCleared;

        boardCopy.move(act);
        return boardCopy;
    }

    @Override
    public Piece getCurrentPiece() { 
        // returns the current piece, including its type and rotation
        return this.currentPiece;
    }

    @Override
    public Point getCurrentPiecePosition() { 
        // return a copy of the piece origin so callers cannot accidentally change its position
        if (currentPiecePosition == null) {
            return null;
        }
        return new Point(currentPiecePosition);
    }

    @Override
    public void nextPiece(Piece p, Point spawnPosition) {
        // check that the piece and spawn position are not null
        if (p == null || p.getType() == null || spawnPosition == null) {
            System.err.println("Piece, piece type, and spawn position cannot be null");
            return;
        }

        // check that every block fits in the board and does not overlap with an already placed piece
        if (!canPlace(p, spawnPosition)) {
            System.err.println("Piece either doesn't fit on the board or overlaps with another piece");
            return;
        }

        // set the current piece and copy its spawn position
        currentPiece = p;
        currentPiecePosition = new Point(spawnPosition);
    }

    // check whether every block of a piece fits at the proposed position
    // in other words, checks whether every block of a piece fits on the board AND doesn't overlap with another piece
    private boolean canPlace(Piece piece, Point position) {
        for (Point bodyPoint : piece.getBody()) {
            int x = position.x + bodyPoint.x;
            int y = position.y + bodyPoint.y;
            if (x < 0 || x >= width || y < 0 || y >= height || grid[x][y] != null) {
                return false;
            }
        }
        return true;
    }

    // rotate the current piece and try each available wall kick until one works
    private Result rotateCurrentPiece(boolean clockwise) {
        // get the piece in the requested rotation
        Piece rotatedPiece;
        if (clockwise) {
            rotatedPiece = currentPiece.clockwisePiece();
        } else {
            rotatedPiece = currentPiece.counterclockwisePiece();
        }

        // choose wall kick positions based on the piece type and rotation direction
        Point[][] kickTests;
        if (currentPiece.getType() == Piece.PieceType.STICK) {
            if (clockwise) {
                kickTests = Piece.I_CLOCKWISE_WALL_KICKS;
            } else {
                kickTests = Piece.I_COUNTERCLOCKWISE_WALL_KICKS;
            }
        } else if (currentPiece.getType() == Piece.PieceType.SQUARE) {
            // the square piece rotates in place and does not use wall kicks
            // make one list for each rotation index, with one offset in each list
            kickTests = new Point[4][1];
            // fill all four lists so kickTests[rotationIndex] works for any rotation
            for (int i = 0; i < 4; i++) {
                // an offset of 0, 0 keeps the piece at its current position
                kickTests[i][0] = new Point(0, 0);
            }
        } else {
            //implements clockwise/counterclockwise wall kicks
            if (clockwise) {
                kickTests = Piece.NORMAL_CLOCKWISE_WALL_KICKS;
            } else {
                kickTests = Piece.NORMAL_COUNTERCLOCKWISE_WALL_KICKS;
            }
        }

        // use the wall kick list for the current rotation and try each position in order
        int rotationIndex = currentPiece.getRotationIndex();
        for (Point kick : kickTests[rotationIndex]) {
            // add the wall kick offset to the current position
            Point proposedPosition = new Point(currentPiecePosition.x + kick.x,
                                               currentPiecePosition.y + kick.y);
            // update the piece and position if the rotated piece fits
            if (canPlace(rotatedPiece, proposedPosition)) {
                currentPiece = rotatedPiece;
                currentPiecePosition = proposedPosition;
                lastResult = Result.SUCCESS;
                return lastResult;
            }
        }

        // return out of bounds if none of the wall kick positions fit
        lastResult = Result.OUT_BOUNDS;
        return lastResult;
    }

    // place the piece by copying the falling piece into the grid and clearing any full rows
    private void placeCurrentPiece() {
        // get the type to store in every cell occupied by this piece
        Piece.PieceType type = currentPiece.getType();
        for (Point bodyPoint : currentPiece.getBody()) {
            // add the body point to the piece origin to get its board coordinates
            int x = currentPiecePosition.x + bodyPoint.x;
            int y = currentPiecePosition.y + bodyPoint.y;
            grid[x][y] = type;
        }

        // the piece has landed, so there is no longer a current falling piece
        currentPiece = null;
        currentPiecePosition = null;

        // move each row that is not full down to the next open row, and skip rows that are full (they aren't removed yet)
        // nextRowToWrite tracks where the next row we keep should go
        int nextRowToWrite = 0;
        for (int row = 0; row < height; row++) {
            if (isRowFull(row)) {
                // count the full row and leave its destination open for the next row
                rowsCleared++;
            } else {
                if (nextRowToWrite != row) {
                    // copy this row down to fill the gap left by skipped full rows
                    for (int x = 0; x < width; x++) {
                        grid[x][nextRowToWrite] = grid[x][row];
                    }
                }
                // this row is kept, so the next kept row goes one row above it
                nextRowToWrite++;
            }
        }

        // clear all the rows left empty at the top
        for (int row = nextRowToWrite; row < height; row++) {
            for (int x = 0; x < width; x++) {
                grid[x][row] = null;
            }
        }

        // update the stored heights and row widths after the grid changes
        updateStats();
    }

    // check whether every cell of a row in the grid is filled
    private boolean isRowFull(int y) {
        for (int x = 0; x < width; x++) {
            if (grid[x][y] == null) {
                return false;
            }
        }
        return true;
    }

    // recompute the cached column heights, row widths, and max height from the grid
    // only called after a piece is placed, since that is the only time the grid changes
    private void updateStats() {
        // reset the cached values before counting again
        for (int y = 0; y < height; y++) {
            rowWidths[y] = 0;
        }

        maxHeight = 0;
        
        // go through the grid one column at a time, from the bottom row to the top row
        for (int x = 0; x < width; x++) {
            columnHeights[x] = 0;
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != null) {
                    // we're moving upward, so the last filled cell we see is the highest, making the column height y + 1
                    columnHeights[x] = y + 1;
                    // every filled cell adds 1 to the width of its row
                    rowWidths[y]++;
                }
            }
            // the max height is the height of the tallest column
            maxHeight = Math.max(maxHeight, columnHeights[x]);
        }
    }

    @Override
    public boolean equals(Object other) { 
        // check that the other object is also a board
        if (!(other instanceof Board)) {
            return false;
        }

        Board otherBoard = (Board) other;
        // boards with different dimensions can never be equvalent
        if (width != otherBoard.getWidth() || height != otherBoard.getHeight()) {
            return false;
        }

        // check that both boards have the same current/falling piece
        Piece otherPiece = otherBoard.getCurrentPiece();
        if (currentPiece != null) {
            if (otherPiece == null) {
                return false;
            }
            if (currentPiece.getType() != otherPiece.getType()) {
                return false;
            }
            if (currentPiece.getRotationIndex() != otherPiece.getRotationIndex()) {
                return false;
            }
        } else if (otherPiece != null) {
            return false;
        }

        // check that both boards have the same current/falling piece position
        Point otherPosition = otherBoard.getCurrentPiecePosition();
        if (currentPiecePosition == null) {
            if (otherPosition != null) {
                return false;
            }
        } else if (!currentPiecePosition.equals(otherPosition)) {
            return false;
        }

        // check that every placed grid cell is the same
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != otherBoard.getGrid(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Result getLastResult() { 
        // returns the result of the last action, like SUCCESS or PLACE
        return lastResult;
    }

    @Override
    public Action getLastAction() { 
        // returns the last action, regardless of whether it failed or succeeded
        return lastAction;
    }

    @Override
    public int getRowsCleared() { 
        // returns how many rows were cleared
        return rowsCleared; 
    }

    @Override
    public int getWidth() { 
        // returns the width of the grid
        return this.width;
    }

    @Override
    public int getHeight() { 
        // returns the height of the grid
        return this.height; 
    }

    @Override
    public int getMaxHeight() { 
        // cached, so this is constant time
        return maxHeight;
    }

    @Override
    public int dropHeight(Piece piece, int x) {
        // check that the piece and its type are not null
        if (piece == null || piece.getType() == null) {
            System.err.println("Piece and piece type cannot be null");
            return 0;
        }

        // allow the piece origin to be below zero when its bottom blocks still fit
        int landingY = Integer.MIN_VALUE;
        // get the lowest block in each column of the piece
        int[] skirt = piece.getSkirt();
        for (int pieceX = 0; pieceX < skirt.length; pieceX++) {
            // skip columns of the piece that do not contain a block
            if (skirt[pieceX] != Integer.MAX_VALUE) {
                // find how high the piece origin must be above this board column
                int columnLandingY = getColumnHeight(x + pieceX) - skirt[pieceX];
                // use the highest required position so the piece clears every column
                landingY = Math.max(landingY, columnLandingY);
            }
        }
        // return the y position where the piece lands
        return landingY;
    }

    @Override
    public int getColumnHeight(int x) { 
        // cached, so this is constant time; columns off the board are empty
        if (x < 0 || x >= width) {
            return 0;
        }
        return columnHeights[x];
    }

    @Override
    public int getRowWidth(int y) { 
        // cached, so this is constant time; rows off the board are empty
        if (y < 0 || y >= height) {
            return 0;
        }
        return rowWidths[y];
    }

    @Override
    public Piece.PieceType getGrid(int x, int y) { 
        // check that x and y are in bounds and then return the value at grid point x, y
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return grid[x][y];
    }
}
