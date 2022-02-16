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
 * Double array implementation
 * Created by Oliver Krauss on 27.07.2016.
 */
public class MinicDoubleArray extends MinicArray<Double> {

    /**
     * Array stored in the object
     */
    private final double[] array;

    /**
     * Constructor accepting size
     * @param size of array to be created (multidimensional)
     */
    public MinicDoubleArray(int[] size) {
        super(size);
        array = new double[totalSize];
    }

    @Override
    public Double getAtPos(int[] pos) {
        return array[getPos(pos)];
    }

    @Override
    public void setAtPos(int[] pos, Double value) {
        array[getPos(pos)] = value;
    }
}
