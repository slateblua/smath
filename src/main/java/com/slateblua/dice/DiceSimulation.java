package com.slateblua.dice;

import com.slateblua.simulation.AbstractSimulation;
import com.slateblua.state.State;
import com.slateblua.state.TransitionMatrix;
import com.slateblua.util.SMathUtils;

/**
 * Markov chain model of the 100-square dice game
 */
public class DiceSimulation extends AbstractSimulation {
    public static final int DEFAULT_BOARD_SIZE = 100;
    private static final int DIE_FACES = 6;
    private static final int BONUS_ROLL_FACE = 6;
    private static final int PENALTY_DIVISOR = 13;

    private final int boardSize;
    private final DiceTransitionModel model;

    public DiceSimulation() {
        this(DEFAULT_BOARD_SIZE);
    }

    public DiceSimulation(int boardSize) {
        if (boardSize <= 0) {
            throw new IllegalArgumentException("Board size must be strictly positive, got: " + boardSize);
        }
        this.boardSize = boardSize;
        this.model = buildTransitionModel();
    }

    /**
     * Builds the transient transition matrix {@code Q} and single-turn finish probabilities.
     */
    public final DiceTransitionModel buildTransitionModel() {
        TransitionMatrix q = new TransitionMatrix(boardSize);
        State finish = State.zeros(boardSize);

        for (int startPosition = 0; startPosition < boardSize; startPosition++) {
            processRoll(startPosition, startPosition, 1.0, q, finish);
        }

        return new DiceTransitionModel(q, finish);
    }

    /**
     * Recursively resolves a single turn starting at {@code startPosition}, where
     * {@code currentPosition} tracks mid-turn movement across bonus rolls of 6.
     */
    private void processRoll(
            int startPosition,
            int currentPosition,
            double prob,
            TransitionMatrix q,
            State finish
    ) {
        int lastSquare = boardSize - 1;

        for (int roll = 1; roll <= DIE_FACES; roll++) {
            double p = prob / DIE_FACES;
            int newPosition = currentPosition + roll;

            // Passing the final square means winning during this turn.
            if (newPosition > lastSquare) {
                finish.add(p, startPosition);
                continue;
            }

            // Landing on a prime moves one additional step.
            if (SMathUtils.isPrime(newPosition)) {
                newPosition += 1;

                // The additional step itself can take us past the final square.
                if (newPosition > lastSquare) {
                    finish.add(p, startPosition);
                    continue;
                }
            }

            // Multiple of 13 means we lose the rest of this turn.
            // The next turn starts at this position.
            if (newPosition % PENALTY_DIVISOR == 0) {
                q.add(p, startPosition, newPosition);
                continue;
            }

            // A 6 gives another roll during the same turn from newPosition,
            // while still crediting transitions to the turn's startPosition row.
            if (roll == BONUS_ROLL_FACE) {
                processRoll(startPosition, newPosition, p, q, finish);
            } else {
                q.add(p, startPosition, newPosition);
            }
        }
    }

    /**
     * Computes the exact probability of finishing the game on {@code turn} (1-indexed),
     */
    public double probabilityFinishOnTurn(int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("Turn must be at least 1, got: " + turn);
        }

        State distribution = State.zeros(boardSize);
        // Beginning of turn 1: player starts at position 0.
        distribution.set(1.0, 0);

        // Advance through turns 1 ... turn - 1 using the library's state transition method.
        distribution = evolveMarkovSteps(model.transientMatrix(), distribution, turn - 1);

        // Probability of finishing during the requested turn.
        return distribution.dot(model.finishProbabilities());
    }

    /**
     * Problem-specific print statement
     */
    public void solve() {
        SMathUtils.printMatrix(model.transientMatrix());
        double probability = probabilityFinishOnTurn(40);

        System.out.printf(java.util.Locale.ROOT,
                "Probability of finishing exactly on turn 40: %.12f%n",
                probability
        );
    }
}
