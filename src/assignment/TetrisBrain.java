package assignment;

import assignment.Board.Action;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

// tries diff places for the current piece and picks the one that leaves the board in good shape
public class TetrisBrain implements Brain {
    // these moves let us move around and rotate before trying a drop
    private static final Action[] SEARCH_ACTIONS = {
        Action.LEFT,
        Action.RIGHT,
        Action.CLOCKWISE,
        Action.COUNTERCLOCKWISE
    };

    // keep the board we're testing and the first move that got us on this path
    private static class State {
        final Board board;
        final Action firstMove;

        State(Board board, Action firstMove) {
            this.board = board;
            this.firstMove = firstMove;
        }
    }

    @Override
    public Action nextMove(Board currentBoard) {
        // no falling piece means there's nothing for the brain to move yet
        if (currentBoard.getCurrentPiece() == null) {
            return Action.NOTHING;
        }

        // queue holds positions we still need to check, set remembers ones we've already checked
        ArrayDeque<State> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        // NOTHING here just means we haven't taken a move from the starting position yet
        State startingState = new State(currentBoard, Action.NOTHING);
        pending.add(startingState);

        String startingKey = positionKey(currentBoard);
        visited.add(startingKey);
        // start below every possible score cuz even a bad board can have the best move available
        double bestScore = Double.NEGATIVE_INFINITY;
        Action bestMove = Action.DROP;

        // bfs checks shorter paths first, so we don't take extra moves to reach the same spot
        while (!pending.isEmpty()) {
            State state = pending.remove();
            // testMove gives us a separate board so this doesn't change the actual game
            // try dropping from this spot and see how good the board looks afterward
            Board dropped = state.board.testMove(Action.DROP);
            double score = scoreBoard(dropped);
            // only replace it for a better score, so ties keep the earlier path
            if (score > bestScore) {
                bestScore = score;
                if (state.firstMove == Action.NOTHING) {
                    // the starting spot is best so we can just drop right now
                    bestMove = Action.DROP;
                } else {
                    // return the first move toward this placement, not the whole path
                    bestMove = state.firstMove;
                }
            }

            for (Action action : SEARCH_ACTIONS) {
                // try each move on a copy and skip it if it hits a wall or another piece
                Board next = state.board.testMove(action);
                if (next.getLastResult() != Board.Result.SUCCESS) {
                    continue;
                }

                String key = positionKey(next);
                // don't keep going left then right back to a spot we've already seen
                if (visited.contains(key)) {
                    continue;
                }
                visited.add(key);

                Action firstMove;
                if (state.firstMove == Action.NOTHING) {
                    // this is the first move from the og position, so remember itirst understand that the function retrieves context, generates a response, saves the interaction, and returns the result.
                    firstMove = action;
                } else {
                    // farther along the path we still need to remember that same first move
                    firstMove = state.firstMove;
                }

                // add this position to the queue so we can check its drop and moves too
                State nextState = new State(next, firstMove);
                pending.add(nextState);
            }
        }
        // next tick the brain runs again using wherever the piece ended up
        return bestMove;
    }

    // placed blocks stay the same while we're searching, so x, y and rotation identify each spot
    private String positionKey(Board board) {
        Point position = board.getCurrentPiecePosition();
        Piece piece = board.getCurrentPiece();
        int rotationIndex = piece.getRotationIndex();
        String key = position.x + "," + position.y + "," + rotationIndex;
        return key;
    }

    // higher score means we'd rather leave the board like this after placing the piece
    private double scoreBoard(Board board) {
        int totalHeight = 0;
        int holes = 0;
        int bumpiness = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            int height = board.getColumnHeight(x);
            // add up all the column heights cuz we want to keep the stack low overall
            totalHeight = totalHeight + height;
            // an empty cell below the top block is a hole and will be harder to fill later
            for (int y = 0; y < height; y++) {
                if (board.getGrid(x, y) == null) {
                    holes++;
                }
            }
            if (x > 0) {
                // compare neighboring columns to see how uneven the top of the stack is
                int previousColumn = x - 1;
                int previousHeight = board.getColumnHeight(previousColumn);
                int heightDifference = Math.abs(height - previousHeight);
                bumpiness = bumpiness + heightDifference;
            }
        }

        // reward clearing rows and take away points for height, holes and an uneven stack
        // these weights just balance the diff things we care about, no future pieces involved
        double clearedRowsReward = 0.8 * board.getRowsCleared();
        double heightPenalty = 0.5 * totalHeight;
        double holePenalty = 0.35 * holes;
        double bumpinessPenalty = 0.18 * bumpiness;

        // combine everything into one score so we can compare the placements
        double score = clearedRowsReward;
        score = score - heightPenalty;
        score = score - holePenalty;
        score = score - bumpinessPenalty;
        return score;
    }
}
