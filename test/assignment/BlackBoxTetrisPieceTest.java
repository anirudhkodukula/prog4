package assignment;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import org.junit.jupiter.api.Test;

import assignment.Piece.PieceType;

/*
 * Any comments and methods here are purely descriptions or suggestions.
 * This is your test file. Feel free to change this as much as you want.
 */

public class BlackBoxTetrisPieceTest {

    // checking if the body contains the points 
     private boolean contains(Point[] body, int x, int y) {
        for (Point p : body) {
            if (p.x == x && p.y == y) {
                return true;
            }
        }
        return false;
    }

    //check if the tetris piece body has the 4 points - order don't matter
     private void assertBody(Piece piece, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        Point[] body = piece.getBody();
        assertEquals(4, body.length);
        assertTrue(contains(body, x1, y1));
        assertTrue(contains(body, x2, y2));
        assertTrue(contains(body, x3, y3));
        assertTrue(contains(body, x4, y4));
    }

    @Test
    void newPieceIsInOgState() {
        Piece t = new TetrisPiece(PieceType.T);
 
        assertEquals(PieceType.T, t.getType());
        assertEquals(0, t.getRotationIndex());
        assertBody(t, 0,1, 1,1, 2,1, 1,2);
    }

    @Test
    void widthAndHeight() {
        assertEquals(2, new TetrisPiece(PieceType.SQUARE).getWidth());
        assertEquals(2, new TetrisPiece(PieceType.SQUARE).getHeight());
        assertEquals(3, new TetrisPiece(PieceType.T).getWidth());
        assertEquals(3, new TetrisPiece(PieceType.T).getHeight());
        assertEquals(4, new TetrisPiece(PieceType.STICK).getWidth());
        assertEquals(4, new TetrisPiece(PieceType.STICK).getHeight());
    }

    @Test
    void tRotatesClockwise() {
        Piece t = new TetrisPiece(PieceType.T);
        Piece r1 = t.clockwisePiece();
        Piece r2 = r1.clockwisePiece();
        Piece r3 = r2.clockwisePiece();
 
        assertEquals(1, r1.getRotationIndex());
        // to the right
        assertBody(r1, 1,0, 1,1, 1,2, 2,1);   
 
        assertEquals(2, r2.getRotationIndex());
        // downwards position
        assertBody(r2, 0,1, 1,1, 2,1, 1,0);   
 
        assertEquals(3, r3.getRotationIndex());
        //to the left
        assertBody(r3, 1,0, 1,1, 1,2, 0,1);   
 
        // Rotating made new pieces; the original is unchanged.
        assertEquals(0, t.getRotationIndex());
        assertBody(t, 0,1, 1,1, 2,1, 1,2);
    }

    @Test
    void stickRotatesClockwise() {
        Piece stick = new TetrisPiece(PieceType.STICK);
        assertBody(stick, 0,2, 1,2, 2,2, 3,2);
 
        stick = stick.clockwisePiece();
        assertBody(stick, 2,0, 2,1, 2,2, 2,3);
 
        stick = stick.clockwisePiece();
        assertBody(stick, 0,1, 1,1, 2,1, 3,1);
 
        stick = stick.clockwisePiece();
        assertBody(stick, 1,0, 1,1, 1,2, 1,3);
    }

    @Test
    void squareRotation() {
        Piece square = new TetrisPiece(PieceType.SQUARE);
        Piece rotated = square.clockwisePiece();
 
        assertBody(rotated, 0,0, 0,1, 1,0, 1,1);
        assertEquals(1, rotated.getRotationIndex());
        assertNotEquals(square, rotated);
    }

    @Test
    void rotatingBackAndForthReturnsToStart() {
        for (PieceType type : PieceType.values()) {
            Piece piece = new TetrisPiece(type);
            // 4 cwTurns makes the piece go back to the original position
            Piece fourClockwise = piece.clockwisePiece().clockwisePiece().clockwisePiece().clockwisePiece();
            assertEquals(piece, fourClockwise);
 
            assertEquals(piece, piece.clockwisePiece().counterclockwisePiece());
            assertEquals(piece, piece.counterclockwisePiece().clockwisePiece());
        }
    }

    @Test
    //index 0 goes to 3 and 3 goes to 0 during the rotations of the piece
    void rotationWrapsAround() {
        Piece piece = new TetrisPiece(PieceType.RIGHT_L);
        Piece atThree = piece.counterclockwisePiece();
 
        assertEquals(3, atThree.getRotationIndex());
        assertEquals(0, atThree.clockwisePiece().getRotationIndex());
    }

    @Test
    void skirt() {
        int MAX = Integer.MAX_VALUE;
 
        
        // col 2 has no blocks so we shall set it to max value
        Piece dog = new TetrisPiece(PieceType.RIGHT_DOG).counterclockwisePiece();
        assertArrayEquals(new int[] {1, 0, MAX}, dog.getSkirt());
 
        assertArrayEquals(new int[] {0, 0}, new TetrisPiece(PieceType.SQUARE).getSkirt());
 
        // when stick has only one column
        Piece verticalStick = new TetrisPiece(PieceType.STICK).clockwisePiece();
        assertArrayEquals(new int[] {MAX, MAX, 0, MAX}, verticalStick.getSkirt());
    }

    @Test
    // we look at both pieces we are comparing in their spawn position after we use setup
    void equalsChecksTypeAndRotation() {
        Piece t = new TetrisPiece(PieceType.T);
 
        
        assertEquals(t, new TetrisPiece(PieceType.T));
        assertEquals(t.clockwisePiece(), new TetrisPiece(PieceType.T).clockwisePiece());
 
        //  they are not equal when they have same type but different rotation and vice versa
        assertNotEquals(t, t.clockwisePiece());
        assertNotEquals(t, new TetrisPiece(PieceType.STICK));
        assertFalse(t.equals(null));
        assertFalse(t.equals("T"));


    }

    @Test
    void counterclockwiseIsSameAsThreeClockwise() {
        for (PieceType type : PieceType.values()) {
            Piece piece = new TetrisPiece(type);
            Piece threeClockwise = piece.clockwisePiece().clockwisePiece().clockwisePiece();
 
            assertEquals(threeClockwise, piece.counterclockwisePiece());
        }
    }

    @Test
    void leftDogRotatesClockwise() {
        Piece z = new TetrisPiece(PieceType.LEFT_DOG);
        assertBody(z, 0,2, 1,2, 1,1, 2,1);
 
        z = z.clockwisePiece();
        assertBody(z, 2,2, 2,1, 1,1, 1,0);
 
        z = z.clockwisePiece();
        assertBody(z, 0,1, 1,1, 1,0, 2,0);
 
        z = z.clockwisePiece();
        assertBody(z, 1,2, 1,1, 0,1, 0,0);
    }

}

