package com.slateblua.util;

import com.slateblua.state.State;
import com.slateblua.state.TransitionMatrix;

import java.util.Locale;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Utility methods for multidimensional indexing, number theory, linear algebra,
 * bit-packing, random waiting-time generation, and matrix formatting.
 */
public final class SMathUtils {

    private SMathUtils() {
    }

    /**
     * Validates a multidimensional shape and returns the total flat element count.
     */
    public static int validateShape(int... shape) {
        if (shape == null || shape.length == 0) {
            throw new IllegalArgumentException("Shape must have at least one dimension");
        }

        int size = 1;
        for (int dimension : shape) {
            if (dimension <= 0) {
                throw new IllegalArgumentException("All dimensions must be strictly positive, got: " + dimension);
            }
            size = Math.multiplyExact(size, dimension);
        }
        return size;
    }

    /**
     * Converts multidimensional coordinates into a row-major flat index.
     */
    public static int toLinearIndex(int[] shape, int... indices) {
        Objects.requireNonNull(shape, "Shape must not be null");

        if (indices == null || indices.length != shape.length) {
            throw new IllegalArgumentException(
                    "Incorrect number of indices: expected " + shape.length
                            + ", got " + (indices == null ? 0 : indices.length)
            );
        }

        int linearIndex = 0;
        for (int axis = 0; axis < shape.length; axis++) {
            int coord = indices[axis];
            if (coord < 0 || coord >= shape[axis]) {
                throw new IndexOutOfBoundsException(
                        "Index " + coord + " out of bounds for dimension " + axis + " of size " + shape[axis]
                );
            }
            linearIndex = linearIndex * shape[axis] + coord;
        }
        return linearIndex;
    }

    /**
     * Converts a row-major flat index back into multidimensional coordinates.
     */
    public static int[] fromLinearIndex(int[] shape, int linearIndex) {
        int totalSize = validateShape(shape);
        if (linearIndex < 0 || linearIndex >= totalSize) {
            throw new IndexOutOfBoundsException(
                    "Linear index " + linearIndex + " out of bounds for size " + totalSize
            );
        }

        int[] coordinates = new int[shape.length];
        int remaining = linearIndex;
        for (int axis = shape.length - 1; axis >= 0; axis--) {
            int dim = shape[axis];
            coordinates[axis] = remaining % dim;
            remaining /= dim;
        }
        return coordinates;
    }

    /**
     * Wraps an index periodically into {@code [0, length)}.
     */
    public static int periodicIndex(int index, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be strictly positive");
        }
        return Math.floorMod(index, length);
    }

    /**
     * Checks whether {@code n} is a prime number using integer trial division.
     */
    public static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        if (n == 2) {
            return true;
        }
        if (n % 2 == 0) {
            return false;
        }
        for (int d = 3; d <= n / d; d += 2) {
            if (n % d == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes the inner product of two equal-length arrays.
     */
    public static double dot(double[] a, double[] b) {
        Objects.requireNonNull(a, "First array must not be null");
        Objects.requireNonNull(b, "Second array must not be null");

        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vector lengths do not match: " + a.length + " vs " + b.length
            );
        }

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Samples an exponentially distributed waiting time {@code -ln(1 - u) / totalRate}
     * for a Poisson process with rate {@code totalRate}.
     */
    public static double sampleExponentialTime(RandomGenerator random, double totalRate) {
        Objects.requireNonNull(random, "Random generator must not be null");

        if (!(totalRate > 0.0) || Double.isInfinite(totalRate)) {
            throw new IllegalArgumentException("Total rate must be finite and strictly positive, got: " + totalRate);
        }

        double u = 1.0 - random.nextDouble();
        return -Math.log(u) / totalRate;
    }

    /**
     * Unpacks the lowest {@code sites} bits of a 64-bit integer into a 1D binary {@link State}.
     */
    public static State stateFromBitmask(long bitmask, int sites) {
        if (sites <= 0 || sites > Long.SIZE) {
            throw new IllegalArgumentException("Sites must be between 1 and 64, got: " + sites);
        }

        State state = State.zeros(sites);
        for (int j = 0; j < sites; j++) {
            state.setFlat(j, (double) ((bitmask >>> j) & 1L));
        }
        return state;
    }

    /**
     * Packs a 1D binary {@link State} (of length at most 64) into a 64-bit bitmask.
     */
    public static long stateToBitmask(State state) {
        Objects.requireNonNull(state, "State must not be null");

        if (state.rank() != 1 || state.size() > Long.SIZE) {
            throw new IllegalArgumentException("Bitmask conversion requires a 1D state of at most 64 sites");
        }

        long mask = 0L;
        for (int j = 0; j < state.size(); j++) {
            int bit = state.getFlatInt(j);
            if (bit != 0 && bit != 1) {
                throw new IllegalArgumentException("State contains non-binary value at index " + j + ": " + bit);
            }
            if (bit == 1) {
                mask |= (1L << j);
            }
        }
        return mask;
    }

    /**
     * Formats a {@link TransitionMatrix} with 6 decimal places per entry.
     */
    public static String formatMatrix(TransitionMatrix matrix) {
        Objects.requireNonNull(matrix, "Matrix must not be null");
        int n = matrix.dimension();
        StringBuilder sb = new StringBuilder(n * n * 9);
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                sb.append(String.format(Locale.ROOT, "%.6f ", matrix.get(row, col)));
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Prints a {@link TransitionMatrix} to standard output.
     */
    public static void printMatrix(TransitionMatrix matrix) {
        System.out.print(formatMatrix(matrix));
    }
}
