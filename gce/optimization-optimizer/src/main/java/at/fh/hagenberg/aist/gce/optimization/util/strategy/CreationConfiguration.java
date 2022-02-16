/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import at.fh.hagenberg.machinelearning.core.Configurable;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains configuration values for the creation of subtrees
 *
 * @author Oliver Krauss on 07.11.2018
 */

public class CreationConfiguration implements Configurable {

    /**
     * maximum depth of a subtree
     */
    protected int maxDepth;

    /**
     * maximum width of an array type (not the entire subtree!)
     */
    protected int maxWidth;

    /**
     * Maximum weight (approximated) that may be created by the strategies.
     * Usually equals the currently best known solution.
     */
    protected double maxWeight = Double.MAX_VALUE;

    public CreationConfiguration() {
        this.maxDepth = 5;
        this.maxWidth = 5;
    }

    public CreationConfiguration(int maxDepth, int maxWidth, double maxWeight) {
        this.maxDepth = maxDepth;
        this.maxWidth = maxWidth;
        this.maxWeight = maxWeight;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public CreationConfiguration setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public CreationConfiguration setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public CreationConfiguration setMaxWeight(double maxWeight) {
        this.maxWeight = maxWeight;
        return this;
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("maxDepth", new Descriptor<>(maxDepth));
        options.put("maxWidth", new Descriptor<>(maxWidth));
        options.put("maxWeight", new Descriptor<>(maxWeight));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "maxDepth":
                    setMaxDepth((int) descriptor.getValue());
                    break;
                case "maxWidth":
                    setMaxWidth((int) descriptor.getValue());
                    break;
                case "maxWeight":
                    setMaxWeight((double) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CreationConfiguration{" +
            "maxDepth=" + maxDepth +
            ", maxWidth=" + maxWidth +
            ", maxWeight=" + maxWeight +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreationConfiguration)) return false;
        CreationConfiguration that = (CreationConfiguration) o;
        return maxDepth == that.maxDepth &&
                maxWidth == that.maxWidth &&
                Double.compare(that.maxWeight, maxWeight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxDepth, maxWidth, maxWeight);
    }
}
