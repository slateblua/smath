package com.slateblua.montecarlo;

import com.slateblua.state.State;

import java.util.Objects;

/**
 * Result of attempting to hop a walker to an adjacent lattice site.
 *
 * @param wasMoved {@code true} if a walker moved into an empty neighbor site
 * @param newState resulting lattice {@link State} (an unmodified copy if blocked)
 */
public record MoveResult(
        boolean wasMoved,
        State newState
) {
    public MoveResult {
        Objects.requireNonNull(newState, "New state must not be null");
    }
}
