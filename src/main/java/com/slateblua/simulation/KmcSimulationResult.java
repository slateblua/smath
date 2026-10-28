package com.slateblua.simulation;

import com.slateblua.state.State;

import java.util.Objects;

/**
 * @param finalState    system state at the end of the run
 * @param elapsedTime   physical time elapsed across the run
 * @param totalSteps    number of rejection KMC trial steps executed
 * @param acceptedSteps number of trial steps whose transitions were accepted
 */
public record KmcSimulationResult(
        State finalState,
        double elapsedTime,
        int totalSteps,
        int acceptedSteps
) {
    public KmcSimulationResult {
        Objects.requireNonNull(finalState, "Final state must not be null");
    }
}
