/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data access for statistical values. Intended to be used for data being transformed into this format
 * and then being processed by the reporting and statistics functions
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class Dataset {

    /**
     * Values of data set
     */
    private double[][] values;

    /**
     * Title of data set
     */
    private String[] title;

    private Map<String, Integer> positions = new HashMap<>();

    public Dataset(double[][] values, String[] title) {
        if (values == null || title == null) {
            throw new IllegalArgumentException("Values and Titles must be set");
        } else if (values.length != title.length) {
            throw new IllegalArgumentException("Each Dataset must have a title");
        }
        this.values = values;
        this.title = title;
        for (int i = 0; i < title.length; i++) {
            positions.put(title[i], i);
        }
    }

    /**
     * Returns the data as double array, as [dataset][rows]
     *
     * @return data
     */
    public double[][] getData() {
        return values;
    }

    /**
     * Returns the titles of the data set as [dataset-title]
     * corresponds to [dataset] from getData
     *
     * @return titles of dataset
     */
    public String[] getTitles() {
        return title;
    }

    /**
     * Returns the data set at the given position
     *
     * @param pos position
     * @return values of dataset or null
     */
    public double[] getData(int pos) {
        if (pos >= 0 && pos < values.length) {
            return values[pos];
        }
        return null;
    }

    /**
     * Returns the data set with the given title
     *
     * @param title to be loaded
     * @return values of that dataset or null
     */
    public double[] getData(String title) {
        if (positions.containsKey(title)) {
            return values[positions.get(title)];
        }
        return null;
    }

    /**
     * gets the title of a dataset at a specific position
     *
     * @param pos to be loaded
     * @return title or null
     */
    public String getTitle(int pos) {
        if (pos >= 0 && pos < title.length) {
            return title[pos];
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dataset dataset = (Dataset) o;

        // check titles and positions
        if (!Arrays.equals(title, dataset.title) &&
            Objects.equals(positions, dataset.positions)) {
            return false;
        }

        // check dataset groups
        if (this.values == dataset.values)
            return true;
        if (this.values == null || dataset.values == null)
            return false;
        int length = dataset.values.length;
        if (values.length != length)
            return false;

        // check sets
        for (int i = 0; i < length; i++) {
            if (!(Arrays.equals(values[i], dataset.values[i])))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(positions);
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + Arrays.hashCode(title);
        return result;
    }
}
