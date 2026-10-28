package com.slateblua.dice;

import com.slateblua.state.State;
import com.slateblua.state.TransitionMatrix;

import java.util.Objects;

public record DiceTransitionModel(
        TransitionMatrix transientMatrix,
        State finishProbabilities
) {
    public DiceTransitionModel {
        Objects.requireNonNull(transientMatrix, "Matrix must not be null");
        Objects.requireNonNull(finishProbabilities, "Finish probabilities must not be null");

        if (finishProbabilities.rank() != 1 || finishProbabilities.size() != transientMatrix.dimension()) {
            throw new IllegalArgumentException("Finish probability state size must match matrix dimension");
        }
    }
}
