package com.slateblua;

@SuppressWarnings("unused")
public class SArray<T> {
    private final int[] shape;
    private final Object[] data;

    public SArray(int... shape) {
        if (shape.length == 0) {
            throw new IllegalArgumentException("Array must have at least one dimension");
        }

        this.shape = shape.clone();

        int size = 1;

        for (int dimension : shape) {
            if (dimension < 0) {
                throw new IllegalArgumentException("Dimensions cannot be negative");
            }

            size = Math.multiplyExact(size, dimension);
        }

        this.data = new Object[size];
    }

    @SuppressWarnings("unchecked")
    public T get(int... indices) {
        return (T) data[toLinearIndex(indices)];
    }

    public void set(T value, int... indices) {
        data[toLinearIndex(indices)] = value;
    }

    public int[] shape() {
        return shape.clone();
    }

    public int size() {
        return data.length;
    }

    private int toLinearIndex(int... indices) {
        if (indices.length != shape.length) {
            throw new IllegalArgumentException("Incorrect number of indices");
        }

        int index = 0;

        for (int i = 0; i < shape.length; i++) {
            if (indices[i] < 0 || indices[i] >= shape[i]) {
                throw new IndexOutOfBoundsException("Index out of bounds at dimension " + i);
            }

            index = index * shape[i] + indices[i];
        }

        return index;
    }
}
