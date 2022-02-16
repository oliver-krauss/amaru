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
 * String array implementation
 * Created by Oliver Krauss on 27.07.2016.
 */
public class MinicStringArray extends MinicArray<String> {

    /**
     * Array stored in the object
     */
    private final String[] array;

    /**
     * Constructor accepting size
     * @param size of array to be created (multidimensional)
     */
    public MinicStringArray(int[] size) {
        super(size);
        array = new String[totalSize];
    }

    @Override
    public String getAtPos(int[] pos) {
        return array[getPos(pos)];
    }

    @Override
    public void setAtPos(int[] pos, String value) {
        array[getPos(pos)] = value;
    }
}
