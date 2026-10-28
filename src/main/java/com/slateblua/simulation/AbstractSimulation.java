package com.slateblua.simulation;

import com.slateblua.state.State;
import com.slateblua.state.TransitionMatrix;
import com.slateblua.util.SMathUtils;

import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Abstract base class providing unified Markov chain state evolution and
 * rejection Kinetic Monte Carlo (KMC) simulation over multidimensional {@link State}s.
 * <p>
 */
public abstract class AbstractSimulation {
    private static final double RATE_TOLERANCE = 1e-12;

    protected final RandomGenerator random;
    private double currentTime;

    protected AbstractSimulation() {
        this(new Random());
    }

    protected AbstractSimulation(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "Random generator must not be null");
        this.currentTime = 0.0;
    }

    /**
     * Given a transition matrix and the current multidimensional state, computes and returns
     * the next state
     */
    public static State computeNextState(TransitionMatrix transitionMatrix, State currentState) {
        Objects.requireNonNull(transitionMatrix, "Transition matrix must not be null");
        Objects.requireNonNull(currentState, "Current state must not be null");

        int n = currentState.size();
        if (transitionMatrix.dimension() != n) {
            throw new IllegalArgumentException(
                    "Transition matrix dimension (" + transitionMatrix.dimension()
                            + ") must match flattened state size (" + n + ")"
            );
        }

        State next = State.zeros(currentState.shape());

        for (int from = 0; from < n; from++) {
            double weight = currentState.getFlat(from);
            if (weight == 0.0) {
                continue;
            }
            for (int to = 0; to < n; to++) {
                double transitionProb = transitionMatrix.get(from, to);
                if (transitionProb != 0.0) {
                    next.addFlat(to, weight * transitionProb);
                }
            }
        }
        return next;
    }

    /**
     * Given the transition matrix and the current state, returns the next state.
     */
    public State nextState(TransitionMatrix transitionMatrix, State currentState) {
        return computeNextState(transitionMatrix, currentState);
    }

    /**
     * Evolves {@code initialState} forward through {@code steps} applications of {@code transitionMatrix}.
     */
    public State evolveMarkovSteps(TransitionMatrix transitionMatrix, State initialState, int steps) {
        Objects.requireNonNull(transitionMatrix, "Transition matrix must not be null");
        Objects.requireNonNull(initialState, "Initial state must not be null");
        if (steps < 0) {
            throw new IllegalArgumentException("Steps must be non-negative, got: " + steps);
        }

        State state = initialState.copy();
        for (int s = 0; s < steps; s++) {
            state = nextState(transitionMatrix, state);
        }
        return state;
    }

    /**
     * Generates a random candidate next state from {@code currentState}.
     * <p>
     * Default dummy implementation returns an unmodified copy of {@code currentState};
     * subclasses override this method for specific physical systems.
     */
    public State generateRandomNextState(State currentState) {
        Objects.requireNonNull(currentState, "Current state must not be null");

        return currentState.copy();
    }

    /**
     * Executes a single rejection KMC step:
     * <ol>
     *     <li>Proposes a candidate state via {@link #generateRandomNextState(State)}</li>
     *     <li>Evaluates its physical transition rate and upper bound</li>
     *     <li>Advances simulation time by {@code -ln(1 - u) / totalAttemptRate}</li>
     *     <li>Accepts the candidate with probability {@code rate / maxEventRate}</li>
     * </ol>
     */
    public KmcStepResult rejectionKmcStep(State currentState) {
        Objects.requireNonNull(currentState, "Current state must not be null");

        State candidateState = generateRandomNextState(currentState);
        double rate = transitionRate(currentState, candidateState);
        double maxRate = maxEventRate(currentState);
        double attemptRate = totalAttemptRate(currentState);

        if (rate < 0.0 || Double.isNaN(rate)) {
            throw new IllegalStateException("Transition rate must be non-negative, got: " + rate);
        }
        if (!(maxRate > 0.0) || Double.isInfinite(maxRate)) {
            throw new IllegalStateException("Maximum event rate must be finite and positive, got: " + maxRate);
        }
        if (rate > maxRate + RATE_TOLERANCE) {
            throw new IllegalStateException(
                    "Transition rate (" + rate + ") exceeds configured maxEventRate (" + maxRate + ")"
            );
        }
        if (!(attemptRate > 0.0) || Double.isInfinite(attemptRate)) {
            throw new IllegalStateException("Total attempt rate must be finite and positive, got: " + attemptRate);
        }

        double deltaTime = SMathUtils.sampleExponentialTime(random, attemptRate);
        currentTime += deltaTime;

        double acceptanceProbability = Math.min(1.0, rate / maxRate);
        boolean accepted = acceptanceProbability > 0.0 && random.nextDouble() < acceptanceProbability;
        State nextState = accepted ? candidateState : currentState.copy();

        return new KmcStepResult(nextState, deltaTime, currentTime, accepted);
    }

    /**
     * Runs rejection KMC steps up to exact simulation time {@code startTime + duration}.
     * If a sampled waiting time overshoots {@code targetTime}, no event occurs before
     * {@code targetTime}, so the prior state is retained and the clock is clamped to {@code targetTime}.
     */
    public KmcSimulationResult runKmcUntilTime(State initialState, double duration) {
        Objects.requireNonNull(initialState, "Initial state must not be null");
        if (duration < 0.0 || Double.isNaN(duration) || Double.isInfinite(duration)) {
            throw new IllegalArgumentException("Duration must be finite and non-negative, got: " + duration);
        }

        State state = initialState.copy();
        double startTime = currentTime;
        double targetTime = startTime + duration;
        int totalSteps = 0;
        int acceptedCount = 0;

        while (currentTime < targetTime) {
            KmcStepResult result = rejectionKmcStep(state);
            if (result.totalTime() > targetTime) {
                currentTime = targetTime;
                break;
            }
            state = result.state();
            totalSteps++;
            if (result.accepted()) {
                acceptedCount++;
            }
        }

        return new KmcSimulationResult(state, currentTime - startTime, totalSteps, acceptedCount);
    }

    /**
     * Physical transition rate {@code W(current -> candidate)}.
     * Defaults to {@code 0.0} when the candidate equals the current state.
     */
    protected double transitionRate(State currentState, State candidateState) {
        return 0.0;
    }

    /**
     * Upper bound {@code r_max} on the rate of any single candidate event from {@code currentState}.
     */
    protected double maxEventRate(State currentState) {
        return 1.0;
    }

    /**
     * Number of candidate elementary event channels {@code M} sampled uniformly by
     * {@link #generateRandomNextState(State)}.
     */
    protected int channelCount(State currentState) {
        return 1;
    }

    /**
     * Total Poisson attempt clock rate {@code R_max = channelCount * maxEventRate}.
     */
    protected double totalAttemptRate(State currentState) {
        int channels = channelCount(currentState);
        if (channels <= 0) {
            throw new IllegalStateException("Channel count must be positive, got: " + channels);
        }
        return channels * maxEventRate(currentState);
    }
}
