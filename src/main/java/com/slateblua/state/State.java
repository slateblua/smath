package com.slateblua.state;

import com.slateblua.util.SMathUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Unified multidimensional numeric state representation.
 * <p>
 * Supports arbitrary dimensions {@code (d_0, d_1, ..., d_{k-1})} backed by a contiguous
 * row-major {@code double[]} buffer, suitable for both continuous probability distributions
 * (Markov chains) and discrete or continuous lattice configurations (Kinetic Monte Carlo).
 */
public final class State implements Shaped {
    private final int[] shape;
    private final double[] data;

    public State(int... shape) {
        int size = SMathUtils.validateShape(shape);
        this.shape = shape.clone();
        this.data = new double[size];
    }

    private State(int[] shape, double[] data, boolean copyArrays) {
        this.shape = copyArrays ? shape.clone() : shape;
        this.data = copyArrays ? data.clone() : data;
    }

    public static State zeros(int... shape) {
        return new State(shape);
    }

    public static State of(double[] flatValues, int... shape) {
        Objects.requireNonNull(flatValues, "Values array must not be null");
        int expectedSize = SMathUtils.validateShape(shape);
        if (flatValues.length != expectedSize) {
            throw new IllegalArgumentException(
                    "Values length " + flatValues.length + " does not match shape size " + expectedSize
            );
        }
        return new State(shape, flatValues, true);
    }

    public double get(int... indices) {
        return data[SMathUtils.toLinearIndex(shape, indices)];
    }

    public int getInt(int... indices) {
        return (int) Math.round(get(indices));
    }

    public void set(double value, int... indices) {
        data[SMathUtils.toLinearIndex(shape, indices)] = value;
    }

    public void add(double delta, int... indices) {
        data[SMathUtils.toLinearIndex(shape, indices)] += delta;
    }

    public double getFlat(int linearIndex) {
        return data[linearIndex];
    }

    public int getFlatInt(int linearIndex) {
        return (int) Math.round(data[linearIndex]);
    }

    public void setFlat(int linearIndex, double value) {
        data[linearIndex] = value;
    }

    public void addFlat(int linearIndex, double delta) {
        data[linearIndex] += delta;
    }

    /**
     * Returns an independent deep copy of this state.
     */
    public State copy() {
        return new State(shape, data, true);
    }

    public double sum() {
        double total = 0.0;
        for (double v : data) {
            total += v;
        }
        return total;
    }

    public double dot(State other) {
        Objects.requireNonNull(other, "Other state must not be null");

        if (this.data.length != other.data.length) {
            throw new IllegalArgumentException(
                    "State sizes do not match: " + this.data.length + " vs " + other.data.length
            );
        }

        return SMathUtils.dot(this.data, other.data);
    }

    @Override
    public int[] shape() {
        return shape.clone();
    }

    @Override
    public int rank() {
        return shape.length;
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof State other)) {
            return false;
        }
        return Arrays.equals(shape, other.shape) && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(shape);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "State{shape=" + Arrays.toString(shape) + ", data=" + Arrays.toString(data) + "}";
    }
}
