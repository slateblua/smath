package com.slateblua.simulation;

import com.slateblua.state.State;

import java.util.Objects;

/**
 * Result of a single rejection Kinetic Monte Carlo trial step.
 *
 * @param state     system state after the step (either the accepted candidate or a copy of the prior state)
 * @param deltaTime physical waiting time {@code dt} elapsed during this step
 * @param totalTime cumulative simulation clock time after adding {@code deltaTime}
 * @param accepted  whether the proposed candidate transition was accepted
 */
public record KmcStepResult(
        State state,
        double deltaTime,
        double totalTime,
        boolean accepted
) {
    public KmcStepResult {
        Objects.requireNonNull(state, "State must not be null");
    }
}
