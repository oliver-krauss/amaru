/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics;

import at.fh.hagenberg.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Helper class that contains a Report made by a statistical test.
 *
 * @author Oliver Krauss on 15.10.2019
 */
public class Report {

    private String name;

    private Map<String, Pair<String, String>> values = new HashMap<>();

    /**
     * Sub-Reports that may be contained by the report (optional)
     */
    private Map<String, Report> childReports = new HashMap<>();

    public Report(String name) {
        this.name = name;
    }

    public void addReport(String name, String type, String value) {
        values.put(name, new Pair<>(type, value));
    }

    public void addReport(String name, int value) {
        this.addReport(name, "int", String.valueOf(value));
    }

    public void addReport(String name, double value) {
        this.addReport(name, "double", String.valueOf(value));
    }

    public void addReport(String name, boolean value) {
        this.addReport(name, "double", String.valueOf(value));
    }

    public void addReport(String name, Report r) {
        if (r.getValue("index") == null) {
            r.addReport("index", this.childReports.size());
        }
        this.childReports.put(name, r);
    }

    /**
     * Returns a specific value. You can directly chain sub-reports with "." -> reportX.count
     *
     * @param name of value to return.
     * @return requested value
     */
    public String getValue(String name) {
        if (name.contains(".")) {
            int index = name.indexOf('.');
            return childReports.get(name.substring(0, index)).getValue(name.substring(index + 1));
        }
        if (values.containsKey(name)) {
            return values.get(name).getValue();
        }
        return null;
    }

    /**
     * Returns a sub-report by name
     *
     * @param name of sub-report
     * @return sub-report
     */
    public Report getReport(String name) {
        return childReports.get(name);
    }

    /**
     * Returns a sub-report by index if it exists. If it doesn't exist it will be created
     * @param index of report
     * @return      report
     */
    public Report getReport(int index) {
        String idx = String.valueOf(index);
        Report child = childReports.values().stream().filter(x -> idx.equals((x.getValue("index")))).findFirst().orElse(null);
        if (child == null) {
            child = new Report(idx);
            child.addReport("index", index);
            this.addReport(idx, child);
        }
        return child;
    }

    public String getName() {
        return name;
    }

    public Map<String, Pair<String, String>> getValues() {
        return values;
    }

    public Map<String, Report> getChildReports() {
        return childReports;
    }

    // region string helper functions

    public int longestString() {
        int size = longestKey();
        int valSize = longestValue();
        return size > valSize ? size : valSize;
    }

    public int longestKey() {
        return values.keySet().stream().mapToInt(String::length).max().orElse(0);
    }

    public int longestValue() {
        return values.values().stream().mapToInt(x -> x.getValue().length()).max().orElse(0);
    }

    public int longestChildTitle() {
        return childReports.keySet().stream().mapToInt(String::length).max().orElse(0);
    }

    public int longestChildKey() {
        return childReports.values().stream().mapToInt(Report::longestKey).max().orElse(0);
    }

    public int longestChildValue() {
        return childReports.values().stream().mapToInt(Report::longestValue).max().orElse(0);
    }

    public int longestChildString() {
        int size = longestChildTitle();
        int valSize = longestChildValue();
        return size > valSize ? size : valSize;
    }

    // endregion
}

