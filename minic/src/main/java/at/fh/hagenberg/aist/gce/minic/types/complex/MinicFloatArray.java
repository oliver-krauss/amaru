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
 * Float array implementation
 * Created by Oliver Krauss on 27.07.2016.
 */
public class MinicFloatArray extends MinicArray<Float> {

    /**
     * Array stored in the object
     */
    private final float[] array;

    /**
     * Constructor accepting size
     * @param size of array to be created (multidimensional)
     */
    public MinicFloatArray(int[] size) {
        super(size);
        array = new float[totalSize];
    }

    @Override
    public Float getAtPos(int[] pos) {
        return array[getPos(pos)];
    }

    @Override
    public void setAtPos(int[] pos, Float value) {
        array[getPos(pos)] = value;
    }
}
