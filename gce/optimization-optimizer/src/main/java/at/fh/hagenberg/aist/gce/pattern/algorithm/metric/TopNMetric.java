/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm.metric;

import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.encoding.TracableBitwisePattern;

import java.util.*;

/**
 * This is an encapsulating metric that always restricts to the top N results.
 */
public class TopNMetric implements Metric {

    /**
     * top pattern map
     */
    private SortedMap<Double, TracableBitwisePattern> topPatterns = Collections.synchronizedSortedMap(new TreeMap<>());


    /**
     * top pattern map
     */
    private SortedMap<Double, TracableBitwisePattern> topFinalPatterns = Collections.synchronizedSortedMap(new TreeMap<>());

    /**
     * Growth list used in IGOR
     */
    List<TracableBitwisePattern> growthList = null;

    /**
     * Final list used in IGOR
     */
    List<TracableBitwisePattern> finalList = null;

    /**
     * The actual metric that determines the rank
     */
    private Metric rankingMetric;

    /**
     * All other metrics that are used for being applicable or being expanded as well.
     */
    private List<Metric> otherMetrics = new LinkedList<>();

    /**
     * How many patterns shall be grown (pseudo greedy approach via best rank)
     */
    private int growCount;

    /**
     * How many patterns we want to have remaining at the end
     */
    private int finalCount;

    /**
     * The metrics can be interpreted in EITHER direction. I.e. sometimes you prefer high overlap, and sometimes a low overlap.
     * LowerIsBetter = TRUE  ranks from 0 .. 1 where 0 is best
     * LowerIsBetter = FALSE ranks from 1 .. 0 where 1 is best
     * (no the ranking is not restricted between 0 and 1).
     */
    private boolean lowerIsBetter;

    public TopNMetric(Metric rankingMetric, int growCount, int finalCount, boolean lowerIsBetter) {
        this.growCount = growCount;
        this.finalCount = finalCount;
        this.rankingMetric = rankingMetric;
        this.lowerIsBetter = lowerIsBetter;
    }

    @Override
    public void init(Map<Integer, TrufflePatternProblem> clusters) {
        rankingMetric.init(clusters);
        otherMetrics.forEach(x -> x.init(clusters));
    }

    public void injectLists(List<TracableBitwisePattern> growthList, List<TracableBitwisePattern> finalPatterns) {
        // initialize growth list
        this.growthList = growthList;
        ArrayList<TracableBitwisePattern> list = new ArrayList<>(growthList);
        this.topPatterns.put(lowerIsBetter ? Double.MAX_VALUE : Double.MIN_VALUE, null);

        // inject all other items
        list.forEach(this::expand);

        // initialize final list
        this.finalList = finalPatterns;
        this.topFinalPatterns.put(lowerIsBetter ? Double.MAX_VALUE : Double.MIN_VALUE, null);
    }

    public List<Metric> getOtherMetrics() {
        return otherMetrics;
    }

    public void setOtherMetrics(List<Metric> otherMetrics) {
        this.otherMetrics = otherMetrics;
    }

    @Override
    public boolean applicable(TracableBitwisePattern pattern) {
        return rankingMetric.applicable(pattern) && otherMetrics.stream().allMatch(x -> x.applicable(pattern)) && checkTopN(pattern, topFinalPatterns, finalList, finalCount);
    }

    @Override
    public boolean expand(TracableBitwisePattern pattern) {
        return rankingMetric.expand(pattern) && otherMetrics.stream().allMatch(x -> x.expand(pattern)) && checkTopN(pattern, topPatterns, growthList, growCount);
    }

    @Override
    public double rank(TracableBitwisePattern pattern) {
        return rankingMetric.rank(pattern);
    }

    private boolean checkTopN(TracableBitwisePattern pattern, SortedMap<Double, TracableBitwisePattern> collection, List<TracableBitwisePattern> list, int limit) {
        double rank = rankingMetric.rank(pattern);
        if (!lowerIsBetter) {
            rank = rank * -1;
        }
        while (collection.containsKey(rank) & !Double.isInfinite(rank)) {
            rank += (lowerIsBetter ? 0.00000001 : -0.00000001);
        }

        if (rank < collection.lastKey() || collection.size() < limit) {
            if (collection.size() >= limit) {
                Double key = collection.lastKey();
                list.remove(collection.get(key));
                collection.remove(key);
            }
            collection.put(rank, pattern);
            return true;
        }
        return false;
    }
}
