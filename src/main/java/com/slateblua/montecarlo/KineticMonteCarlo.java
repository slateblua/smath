package com.slateblua.montecarlo;

import com.slateblua.simulation.AbstractSimulation;
import com.slateblua.simulation.KmcSimulationResult;
import com.slateblua.state.State;
import com.slateblua.util.SMathUtils;

import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Rejection Kinetic Monte Carlo simulation of a 1D exclusion process with particle
 * addition, removal, and nearest-neighbor hopping, built on {@link AbstractSimulation}.
 */
public class KineticMonteCarlo extends AbstractSimulation {
    public static final int DEFAULT_SITES = 64;

    private final int sites;
    private final double addRate;
    private final double removeRate;
    private final double moveRate;
    private final boolean periodic;
    private final double maxRate;

    public KineticMonteCarlo() {
        this(new Random());
    }

    public KineticMonteCarlo(RandomGenerator random) {
        this(
                DEFAULT_SITES,
                Operations.ADD_RATE,
                Operations.REMOVE_RATE,
                Operations.MOVE_RATE,
                true,
                random
        );
    }

    public KineticMonteCarlo(
            int sites,
            double addRate,
            double removeRate,
            double moveRate,
            boolean periodic,
            RandomGenerator random
    ) {
        super(random);
        if (sites <= 0) {
            throw new IllegalArgumentException("Number of sites must be strictly positive, got: " + sites);
        }
        if (addRate < 0.0 || removeRate < 0.0 || moveRate < 0.0) {
            throw new IllegalArgumentException("Rates must be non-negative");
        }
        double max = Math.max(addRate, Math.max(removeRate, moveRate));
        if (!(max > 0.0) || Double.isInfinite(max)) {
            throw new IllegalArgumentException("At least one transition rate must be finite and positive");
        }
        this.sites = sites;
        this.addRate = addRate;
        this.removeRate = removeRate;
        this.moveRate = moveRate;
        this.periodic = periodic;
        this.maxRate = max;
    }

    public int sites() {
        return sites;
    }

    /**
     * Proposes a random next lattice state by uniformly choosing a site {@code j}
     * and one of the four elementary {@link Event} types.
     */
    @Override
    public State generateRandomNextState(State currentState) {
        validateState(currentState);

        int j = random.nextInt(sites);
        Event type = randomEventType();

        return switch (type) {
            case ADD -> Operations.get(currentState, j) == 0
                    ? Operations.flip(currentState, j).newState()
                    : currentState.copy();
            case REMOVE -> Operations.get(currentState, j) == 1
                    ? Operations.flip(currentState, j).newState()
                    : currentState.copy();
            case MOVE_LEFT -> Operations.movl(currentState, j, periodic).newState();
            case MOVE_RIGHT -> Operations.movr(currentState, j, periodic).newState();
        };
    }

    @Override
    protected double transitionRate(State currentState, State candidateState) {
        validateState(currentState);
        validateState(candidateState);

        int added = 0;
        int removed = 0;
        for (int i = 0; i < sites; i++) {
            int before = currentState.getFlatInt(i);
            int after = candidateState.getFlatInt(i);
            if (before == 0 && after == 1) {
                added++;
            } else if (before == 1 && after == 0) {
                removed++;
            }
        }

        if (added == 0 && removed == 0) {
            return 0.0;
        }
        if (added == 1 && removed == 0) {
            return addRate;
        }
        if (added == 0 && removed == 1) {
            return removeRate;
        }
        if (added == 1 && removed == 1) {
            return moveRate;
        }
        throw new IllegalStateException(
                "Invalid single-step lattice transition (added=" + added + ", removed=" + removed + ")"
        );
    }

    @Override
    protected double maxEventRate(State currentState) {
        return maxRate;
    }

    @Override
    protected int channelCount(State currentState) {
        return Event.values().length * sites;
    }

    /**
     * Executes the rejection KMC simulation over physical time {@code duration} starting from {@code initialState}.
     */
    public KmcSimulationResult executeUntilTime(State initialState, double duration) {
        validateState(initialState);
        return runKmcUntilTime(initialState, duration);
    }

    /**
     * Convenience overload executing over physical time {@code duration} from a 64-bit bitmask initial state.
     */
    public KmcSimulationResult executeUntilTime(long initialBitmask, double duration) {
        State initialState = SMathUtils.stateFromBitmask(initialBitmask, sites);
        return executeUntilTime(initialState, duration);
    }

    private Event randomEventType() {
        Event[] events = Event.values();
        return events[random.nextInt(events.length)];
    }

    private void validateState(State state) {
        Objects.requireNonNull(state, "State must not be null");
        if (state.rank() != 1 || state.size() != sites) {
            throw new IllegalArgumentException(
                    "Expected 1D state of length " + sites + ", got rank=" + state.rank() + ", size=" + state.size()
            );
        }
    }
}
