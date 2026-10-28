package com.slateblua.montecarlo;

import com.slateblua.state.State;

import java.util.Objects;

/**
 * Result of flipping occupation at a lattice site.
 *
 * @param eventType {@code true} if a particle was added (0 -> 1), {@code false} if removed (1 -> 0)
 * @param newState  newly created lattice {@link State}
 */
public record FlipResult(
        // Termination is denoted by 'false', and addition is denoted by 'true'
        boolean eventType,
        State newState
) {
    public FlipResult {
        Objects.requireNonNull(newState, "New state must not be null");
    }
}
