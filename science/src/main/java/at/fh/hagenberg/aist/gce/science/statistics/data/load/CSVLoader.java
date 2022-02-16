/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.load;

import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.LinkedList;

/**
 * Loader that transforms a valid CSV String to a Dataset
 *
 * @author Oliver Krauss on 24.10.2019
 */
public class CSVLoader implements Loader {

    /**
     * Separator between single values of the CSV (Ex. 1,2,3)
     */
    private String separator = ",";

    /**
     * String match where the CSV to be loaded begins in the reader
     * If not set CSV will be read from the first line
     * Note that the read will EXCLUDE the "begin" line
     */
    private String begin = null;

    /**
     * String match where the CSV to be loaded ends in the reader
     * If not set CSV will be read until the end of the stream
     * Note that the read will EXCLUDE the "end" line
     */
    private String end = null;

    /**
     * Reader that the CSV will be read from
     */
    private Reader reader;

    @Override
    public Dataset load() {
        if (reader == null) {
            return null;
        }

        BufferedReader reader = new BufferedReader(this.reader);
        String[] titles = null;
        double[][] values = null;

        try {

            String val = "";

            // scroll to begin
            if (begin != null && !begin.isEmpty()) {
                while ((val = reader.readLine()) != null && !val.contains(begin)) {
                }
            }

            // read titles
            titles = reader.readLine().split(separator);

            // read values
            // store as lists in between so we won't need to resize arrays (lazy I know)
            LinkedList[] vals = new LinkedList[titles.length];
            for (int i = 0; i < vals.length; i++) {
                vals[i] = new LinkedList<String>();
            }

            // load the values
            while ((val = reader.readLine()) != null) {
                if (end != null && !end.isEmpty() && end.contains(val)) {
                    break;
                }

                String[] split = val.split(separator);
                for (int i = 0; i < split.length; i++) {
                    vals[i].addLast(split[i]);
                }
            }

            // list to array
            values = new double[titles.length][];
            for (int i = 0; i < values.length; i++) {
                values[i] = vals[i].stream().filter(x -> x != null && !((String) x).isEmpty()).mapToDouble(x -> Double.valueOf((String) x)).toArray();
            }
        } catch (Exception e) {
            System.err.println("Parsing of CSV failed");
            e.printStackTrace();
        }

        return new Dataset(values, titles);
    }

    @Override
    public Reader getReader() {
        return reader;
    }

    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
