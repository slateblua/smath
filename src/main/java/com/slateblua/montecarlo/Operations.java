package com.slateblua.montecarlo;

import com.slateblua.state.State;
import com.slateblua.util.SMathUtils;

import java.util.Objects;

public final class Operations {
    private Operations() {
    }

    public static final double ADD_RATE = 0.1;
    public static final double REMOVE_RATE = 0.4;
    public static final double MOVE_RATE = 0.3;

    /**
     * Returns site occupation ({@code 1} if occupied, {@code 0} if empty) at index {@code j}.
     */
    public static int get(State state, int j) {
        validate1DState(state);
        return state.getInt(j);
    }

    /**
     * Flips occupation of site {@code j} ({@code 0 -> 1} or {@code 1 -> 0}) on a copy of {@code state}.
     */
    public static FlipResult flip(State state, int j) {
        validate1DState(state);
        boolean wasAdded = state.getInt(j) == 0;
        State next = state.copy();
        next.set(wasAdded ? 1.0 : 0.0, j);
        return new FlipResult(wasAdded, next);
    }

    /**
     * Attempts to move a walker rightwards (from {@code j} to {@code j - 1}).
     */
    public static MoveResult movr(State state, int j, boolean periodic) {
        validate1DState(state);
        int sites = state.size();
        if (j < 0 || j >= sites) {
            throw new IndexOutOfBoundsException("Site index out of bounds: " + j);
        }

        if (!periodic && j == 0) {
            return new MoveResult(false, state.copy());
        }

        int target = periodic ? SMathUtils.periodicIndex(j - 1, sites) : (j - 1);
        return moveBetweenSites(state, j, target);
    }

    /**
     * Attempts to move a walker leftwards (from {@code j} to {@code j + 1}).
     */
    public static MoveResult movl(State state, int j, boolean periodic) {
        validate1DState(state);
        int sites = state.size();
        if (j < 0 || j >= sites) {
            throw new IndexOutOfBoundsException("Site index out of bounds: " + j);
        }

        if (!periodic && j == sites - 1) {
            return new MoveResult(false, state.copy());
        }

        int target = periodic ? SMathUtils.periodicIndex(j + 1, sites) : (j + 1);
        return moveBetweenSites(state, j, target);
    }

    private static MoveResult moveBetweenSites(State state, int from, int to) {
        if (from == to) {
            return new MoveResult(false, state.copy());
        }

        boolean hasWalker = state.getInt(from) == 1;
        boolean hasSpace = state.getInt(to) == 0;

        if (hasWalker && hasSpace) {
            State next = state.copy();
            next.set(0.0, from);
            next.set(1.0, to);
            return new MoveResult(true, next);
        }

        return new MoveResult(false, state.copy());
    }

    private static void validate1DState(State state) {
        Objects.requireNonNull(state, "State must not be null");
        if (state.rank() != 1) {
            throw new IllegalArgumentException("Expected a 1D lattice state, got rank " + state.rank());
        }
    }
}
