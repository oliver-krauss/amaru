/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.runtime;


import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import java.util.Arrays;

/**
 * Runtime Profile (essentially a box-plot) of a specific run.
 * The runtime profile is a NON-REUSABLE node entity as it is to be expected to be different every single run
 * <p>
 * Pretty much all values in this class (except the field values) are calculated, but stored in the db for conveniences sake
 */
@NodeEntity
public class RuntimeProfile {

    @Transient
    public static final RuntimeProfile FAILED_PROFILE = createFailedProfile();

    private static RuntimeProfile createFailedProfile() {
        long[] fail = new long[1];
        fail[0] = Long.MAX_VALUE;
        return new RuntimeProfile(fail);
    }

    /**
     * Id generated by database
     */
    @Id
    private Long id;

    /**
     * amount of values this profile makes up
     */
    private int count;

    /**
     * All runtime-values of every evaluation that makes up this runtime profile
     */
    // NOTE: Excluded for now, as it MURDERS our DB and Runs above 200.000 runs
    //private long[] values;

    /**
     * The groups define how many nodes are in which of the following groups:
     * 0 - Lower Outliers - from the MIN-Value to the Lower Outer Fence
     * 1 - Lower Suspected Outliers - from the Lower Outer Fence to the Lower Inner Fence
     * 2 - Lower Inner Fence - from the Lower Inner Fence to the 1st Quartile
     * 3 - Lower Quarter - From the 1st quartile to the median
     * 4 - Upper Quarter - From the median to the 3rd Quartile
     * 5 - Upper Inner Fence - from the 3rd Quartile to the Upper Inner Fence
     * 6 - Upper Suspected Outliers - from the Upper Inner Fence to the Upper Outer Fence
     * 7 - Upper Outliers - from the Upper Outer Fence to the MAX Value.
     * <p>
     * Note: Border-Points will be counted towards the area closest to the median,
     * The median-points themselves will be counted to the lower quarter
     */
    private int[] groups = new int[8];

    /**
     * Median of the values field
     */
    private double median;

    /**
     * Mean of the values field
     */
    private double mean;

    /**
     * First quartile of the values field
     */
    private double firstQuartile;

    /**
     * Third quartile of the values field
     */
    private double thirdQuartile;

    /**
     * Minimum of the values field
     */
    private long minimum;

    /**
     * Maximum of the values field
     */
    private long maximum;

    /**
     * Standard deviation of all values
     */
    private double standardDeviation;

    /**
     * Standard deviation of values with definite known upper outliers removed
     * (e.g. where we know that the GC interfered)
     */
    private double standardDeviationNoOutliers;

    /**
     * System Information identifier (as system info is in separate databse -> not the real object
     */
    private String systemInformation = SystemInformation.getCurrentSystem().toString();

    /**
     * Constructor for DB. DO NOT USE OTHERWISE
     */
    public RuntimeProfile() {
    }

    public RuntimeProfile(long[] values) {
        Arrays.sort(values);
        this.count = values.length;
        this.minimum = values[0];
        this.maximum = values[count - 1];
        this.mean = Arrays.stream(values).sum() / (double) this.count;
        this.median = quartile(values, 50);
        this.firstQuartile = quartile(values, 25);
        this.thirdQuartile = quartile(values, 75);
        this.standardDeviation = Math.sqrt(Arrays.stream(values).mapToDouble(x ->  Math.pow(x - this.mean, 2)).sum() / count);

        // group points
        double iqr = this.thirdQuartile - this.firstQuartile;
        for (int i = 0; i < count; i++) {
            long val = values[i];
            if (val < (firstQuartile - iqr - iqr)) {
                groups[0]++;
            } else if (val < firstQuartile - iqr) {
                groups[1]++;
            } else if (val < firstQuartile) {
                groups[2]++;
            } else if (val <= median) {
                groups[3]++;
            } else if (val <= thirdQuartile) {
                groups[4]++;
            } else if (val <= thirdQuartile + iqr) {
                groups[5]++;
            } else if (val <= thirdQuartile + iqr + iqr) {
                groups[6]++;
            } else {
                groups[7]++;
            }
        }

        this.standardDeviationNoOutliers = Math.sqrt(Arrays.stream(values).filter(x -> x <= thirdQuartile + iqr + iqr).mapToDouble(x ->  Math.pow(x - this.mean, 2)).sum() / (count - groups[7]));
    }

    private double quartile(long[] values, double lowerPercent) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("We need at least ONE value to calculate quartiles");
        }
        int position = (int) Math.ceil(values.length * lowerPercent / 100) - 1;
        if (count % 2 == 1) {
            return values[position];
        } else {
            return (values[position] + values[position + 1]) / 2.0;
        }

    }

    public Long getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    /*public long[] getValues() {
        return values;
    }*/

    public double getMedian() {
        return median;
    }

    public double getMean() {
        return mean;
    }

    public double getFirstQuartile() {
        return firstQuartile;
    }

    public double getThirdQuartile() {
        return thirdQuartile;
    }

    public long getMinimum() {
        return minimum;
    }

    public long getMaximum() {
        return maximum;
    }

    public int[] getGroups() {
        return groups;
    }

    public String getSystemInformation() {
        return systemInformation;
    }

    public int[] getQuartiles() {
        return groups;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getStandardDeviationNoOutliers() {
        return standardDeviationNoOutliers;
    }

    public void report() {
        System.out.println("Min:     " + minimum);
        System.out.println("1st Q:   " + firstQuartile);
        System.out.println("Median : " + median);
        System.out.println("Average: " + mean);
        System.out.println("3rd Q:   " + thirdQuartile);
        System.out.println("Maximum: " + maximum);
        System.out.println("Std.Dev: " + standardDeviation);
        System.out.println("Std.Dev (no outliers) : " + standardDeviationNoOutliers);
        System.out.println("Quartile counts:");
        for (int group : groups) {
            System.out.println("  " + group);
        }
    }
}
