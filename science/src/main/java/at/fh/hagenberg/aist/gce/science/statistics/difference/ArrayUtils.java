/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.difference;

/**
 * @author Oliver Krauss on 16.10.2019
 */

public class ArrayUtils {

    /**
     * Flips rows and columns for a given array
     *
     * @param array to flip
     * @return flipped array
     */
    public static double[][] flip(double[][] array) {
        double[][] flip = new double[array[0].length][];
        for (int i = 0; i < flip.length; i++) {
            flip[i] = new double[array.length];
        }
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < flip.length; j++) {
                flip[j][i] = array[i][j];
            }
        }
        return flip;
    }

    /**
     * Flattens an array into a one dimensional array
     *
     * @param array to flatten
     * @return one dimensional array
     */
    public static double[] flatten(double[][] array) {
        int columns = array[0].length;
        int rows = array.length;
        double[] flat = new double[rows * columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                flat[i * columns + j] = array[i][j];
            }
        }
        return flat;
    }

    public static double[] rank(double[] array) {
        double[] ranks = new double[array.length];
        // do the rank calculation
        double currentValue = array[0];
        int lowerRange = 0;
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] != currentValue) {
                // set rankings
                double rank = ((i - lowerRange) / 2.0) + lowerRange + 0.5;
                for (int upd = lowerRange; upd < i; upd++) {
                    ranks[upd] = rank;
                }

                // init new ranking system
                lowerRange = i;
                currentValue = array[i];
            }
        }
        double rank = ((array.length - lowerRange) / 2.0) + lowerRange + 0.5;
        for (int upd = lowerRange; upd < array.length; upd++) {
            ranks[upd] = rank;
        }
        return ranks;
    }
}
