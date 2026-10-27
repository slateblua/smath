package com.slateblua.state;

import com.slateblua.util.SMathUtils;

/**
 * Common contract for multidimensional data structures with a fixed shape.
 */
public interface Shaped {

    /**
     * Returns a defensive copy of the dimensions of this structure.
     */
    int[] shape();

    /**
     * Returns the number of dimensions (rank) of this structure.
     */
    int rank();

    /**
     * Returns the total number of elements across all dimensions.
     */
    int size();

    /**
     * Converts multidimensional coordinates into a row-major linear index.
     */
    default int toLinearIndex(int... indices) {
        return SMathUtils.toLinearIndex(shape(), indices);
    }

    /**
     * Converts a row-major linear index into multidimensional coordinates.
     */
    default int[] fromLinearIndex(int linearIndex) {
        return SMathUtils.fromLinearIndex(shape(), linearIndex);
    }
}
