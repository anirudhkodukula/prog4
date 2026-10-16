package assignment;

/** Runs the normal Tetris game with the brain choosing each action. */
public class JBrainTetris extends JTetris {
    private static final long serialVersionUID = 1L;
    private final Brain brain = new TetrisBrain();

    @Override
    public void tick(Board.Action action) {
        if (gameOn) {
            Board.Action nextAction = brain.nextMove(board);
            super.tick(nextAction);
        }
    }

    public static void main(String[] args) {
        Runnable launchGame = new Runnable() {
            @Override
            public void run() {
                JBrainTetris game = new JBrainTetris();
                createGUI(game);
            }
        };
        javax.swing.SwingUtilities.invokeLater(launchGame);
    }
}
