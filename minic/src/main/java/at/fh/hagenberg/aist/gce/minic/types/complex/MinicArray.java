/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.types.complex;

/**
 * Base array type for Mini C. Supports multidimensional arrays
 * Created by Oliver Krauss on 27.07.2016.
 */
public abstract class MinicArray<T> {

    /**
     * Represents the sizes of all array-dimensions
     */
    private final int[] size;

    /**
     * multipliers for multidimensional access to array
     * Example size[5][11][7] -> multipliers[5*7, 7, 1]
     */
    private final int[] multipliers;

    /**
     * Complete length of the array (currently array sizes are unchangeable)
     */
    protected final int totalSize;

    public MinicArray(int[] size) {
        if (size == null || size.length == 0) {
            throw new AssertionError("Array may not have a negative or no size");
        }
        this.size = size;

        multipliers = new int[size.length];

        int totalSize = 1;
        multipliers[size.length - 1] = 1;
        for (int i = size.length - 1; i >= 0; i--) {
            int subSize = size[i];
            if (subSize < 1) {
                throw new AssertionError("Array may not have a negative or no size");
            }
            totalSize *= subSize;
            if (i > 0) {
                multipliers[i - 1] = totalSize;
            }
        }
        this.totalSize = totalSize;
    }

    protected int getPos(int[] pos) {
        if (pos.length != size.length) {
            throw new AssertionError("C only allows single point access to arrays!");
        }
        int finalPos = 0;
        for (int i = 0; i < size.length; i++) {
            finalPos += pos[i] * multipliers[i];
        }
        if (finalPos >= totalSize) {
            // Just like real c we only calculate towards pos in memory, and don't check for out of bounds per dimension
            throw new AssertionError("Array Out of Bounds exception");
        }
        return finalPos;
    }

    /**
     * Gets a value in the array
     * @param pos position to be returned
     * @return    value at position
     */
    public abstract T getAtPos(int[] pos);

    /**
     * Sets a value in the array
     * @param pos   position to be set
     * @param value to be added to array at pos
     */
    public abstract void setAtPos(int[] pos, T value);
}
