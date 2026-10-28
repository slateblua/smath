package com.slateblua.state;

import com.slateblua.util.SMathUtils;

import java.util.Arrays;

/**
 * Square transition matrix {@code T} of dimension {@code N x N}, where {@code T[i][j]}
 * represents the transition probability from flattened state index {@code i} to {@code j}.
 */
public final class TransitionMatrix implements Shaped {
    private final int dimension;
    private final double[] data;

    public TransitionMatrix(int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("Matrix dimension must be strictly positive, got: " + dimension);
        }
        int totalEntries = Math.multiplyExact(dimension, dimension);
        this.dimension = dimension;
        this.data = new double[totalEntries];
    }

    public int dimension() {
        return dimension;
    }

    public double get(int fromIndex, int toIndex) {
        checkBounds(fromIndex, toIndex);
        return data[fromIndex * dimension + toIndex];
    }

    public void add(double delta, int fromIndex, int toIndex) {
        checkBounds(fromIndex, toIndex);
        data[fromIndex * dimension + toIndex] += delta;
    }

    @Override
    public int[] shape() {
        return new int[]{dimension, dimension};
    }

    @Override
    public int rank() {
        return 2;
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    public int toLinearIndex(int... indices) {
        return SMathUtils.toLinearIndex(new int[]{dimension, dimension}, indices);
    }

    @Override
    public int[] fromLinearIndex(int linearIndex) {
        return SMathUtils.fromLinearIndex(new int[]{dimension, dimension}, linearIndex);
    }

    private void checkBounds(int row, int col) {
        if (row < 0 || row >= dimension || col < 0 || col >= dimension) {
            throw new IndexOutOfBoundsException(
                    "Matrix indices (" + row + ", " + col + ") out of bounds for dimension " + dimension
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransitionMatrix other)) {
            return false;
        }
        return dimension == other.dimension && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(dimension);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "TransitionMatrix{dimension=" + dimension + "x" + dimension + "}";
    }
}
