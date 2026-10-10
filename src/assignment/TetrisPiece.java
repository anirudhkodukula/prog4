package assignment;

import java.awt.*;

import assignment.Piece.PieceType;

/**
 * An immutable representation of a tetris piece in a particular rotation.
 * 
 * All operations on a TetrisPiece should be constant time, except for it's
 * initial construction. This means that rotations should also be fast - calling
 * clockwisePiece() and counterclockwisePiece() should be constant time! You may
 * need to do precomputation in the constructor to make this possible.
 */

public final class TetrisPiece implements Piece {

    private PieceType type;
    private int rotation;
    private int width;
    private int height;
    private Point[] body;
    private int[] skirt;
    // variables for clockwise and counterclockwise rotations
    private TetrisPiece cwTurn;
    private TetrisPiece ccwTurn;
    

    /**
     * Construct a tetris piece of the given type. The piece should be in it's spawn orientation,
     * i.e., a rotation index of 0.
     * 
     * You may freely add additional constructors, but please leave this one - it is used both in
     * the runner code and testing code.
     */
    public TetrisPiece(PieceType type) {
        // TODO: Implement me.
        if (type == null){
            System.err.println("the type can't be null");
            body = new Point[0];
            skirt = new int[0];
            cwTurn = this;
            ccwTurn = this;
            return;
        }

        //made the piece
        setup(type, 0, type.getSpawnBody());

        //going to count the 3 rotations the piece can move
        TetrisPiece current = this;
        for (int i = 1; i < 4; i++) {
            Point[] newBody = rotateClockwise(current.body, width);
            TetrisPiece rotated = new TetrisPiece(type, i, newBody);
 
            current.cwTurn = rotated;
            rotated.ccwTurn = current;
            current = rotated;
        }
        // current is in rotation before going back to og position so we need to set it back to this which is the first position
        current.cwTurn = this;
        this.ccwTurn = current;
    }

    private TetrisPiece(PieceType type, int rotationIndex, Point[] body) {
        setup(type, rotationIndex, body);
    }



    //method to make the piece for each rotation type cuz a piece can have 4 diff positions
        private void setup(PieceType type, int rotationIndex, Point[] body) {
        this.type = type;
        this.rotation = rotationIndex;
        this.width = type.getBoundingBox().width;
        this.height = type.getBoundingBox().height;
        this.body = body;
        // find the smallest value for every column and u start with a huge number and replace it with something that's smaller
        skirt = new int[width];
        for (int x = 0; x < skirt.length; x++) {
            // need this because it's greater than every possible value
            skirt[x] = Integer.MAX_VALUE;
        }
        for (Point p : body) {
            if (p.y < skirt[p.x]) {
                skirt[p.x] = p.y;
            }
        }            
    }

    // method to help a piece rotate clockwise so it can go from one position to the other 3
    private static Point[] rotateClockwise(Point[] body, int n) {
        Point[] rotateBody = new Point[body.length];
        for (int i = 0; i < body.length; i++) {
            //x,y maps to y, n -1 - x for nxn box
            rotateBody[i] = new Point(body[i].y, n - 1 - body[i].x);
        }
        return rotateBody;
    }

    @Override
    public PieceType getType() {
        // TODO: Implement me.
        return type;
    }

    @Override
    public int getRotationIndex() {
        // TODO: Implement me.
        return rotation;
    }

    @Override
    public Piece clockwisePiece() {
        // TODO: Implement me.
        return cwTurn;
    }

    @Override
    public Piece counterclockwisePiece() {
        // TODO: Implement me.
        return ccwTurn;
    }

    @Override
    public int getWidth() {
        // TODO: Implement me.
        return width;
    }

    @Override
    public int getHeight() {
        // TODO: Implement me.
        return height;
    }

    @Override
    public Point[] getBody() {
        // TODO: Implement me.
        return body;
    }

    @Override
    public int[] getSkirt() {
        // TODO: Implement me.
        return skirt;
    }

    @Override
    public boolean equals(Object other) {
        // Ignore objects which aren't also tetris pieces.
        if(!(other instanceof TetrisPiece)) return false;
        TetrisPiece otherPiece = (TetrisPiece) other;

        // TODO: Implement me.
        return type == otherPiece.type && rotation == otherPiece.rotation;
    }
}
