/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.other;

import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The grafting strategy is a specialization of the Known Value Strategy being depth sensitive when providing grafts
 *
 * @author Oliver Krauss on 21.11.2018
 */
public class GraftingStrategy<T extends Node> extends KnownValueStrategy<T> implements TruffleHierarchicalStrategy<T> {

    /**
     * Strategy to access the patterns in the system
     */
    private TruffleMasterStrategy rootStrategy;

    private class GraftInfo {
        public int depth;
        public double weight;
        T val;

        public GraftInfo(int depth, double weight, T val) {
            this.depth = depth;
            this.weight = weight;
            this.val = val;
        }
    }

    private NodeWrapperWeightUtil weightUtil;

    private List<GraftInfo> grafts;

    public GraftingStrategy(Collection<T> values, NodeWrapperWeightUtil weightUtil) {
        super(values);
        this.weightUtil = weightUtil;
        grafts = new LinkedList<>();
        values.forEach(val -> {
            int depth = ExtendedNodeUtil.maxDepth(val);
            double weight = weightUtil != null ? weightUtil.weight(val) : 0;
            // TODO #229 create dfg for this graft and introduce available items (resolves requirements)

            grafts.add(new GraftInfo(depth, weight, val));
        });
    }

    @Override
    public KnownValueStrategy<T> clone() {
        return new GraftingStrategy<T>(new LinkedList<>(values), weightUtil);
    }

    @Override
    public void addValue(T value) {
        super.addValue(value);
        int depth = ExtendedNodeUtil.maxDepth(value);
        double weight = weightUtil != null ? weightUtil.weight(value) : 0;
        grafts.add(new GraftInfo(depth, weight, value));
    }

    @Override
    public void removeValue(T value) {
        super.removeValue(value);
        grafts.removeIf(x -> x.val == value);
    }

    private Stream<T> getPossibilities(CreationInformation information) {
        return filterPossibilities(information)
            .map(x -> x.val) // map to T
            .filter(x -> information.getClazz().isAssignableFrom(x.getClass())); // select only valid objects
    }

    private Stream<GraftInfo> filterPossibilities(CreationInformation information) {
        return grafts.stream()
            .filter(x -> x.depth < information.getConfiguration().getMaxDepth() - information.getCurrentDepth() // only check grafts <= depth
                && x.weight <= information.getConfiguration().getMaxWeight() - information.getCurrentWeight());  // only check grafts <= available weight;
    }

    @Override
    public T create(CreationInformation information) {
        // TODO #229 on create fulfill the requirements that the graft supports
        List<T> options = getPossibilities(information).collect(Collectors.toList());
        return options.get(RandomUtil.random.nextInt(options.size()));
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        // TODO #229 the Grafting Strategy does NOT check if the grafts fulfill requirements; it also does not FIX them on create!
        return getPossibilities(information).count() > 0 ? information.getRequirements() : null;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return canCreate(information);
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        // TODO #229 LEFT and RIGHT paths are same as regular RandomReflectiveSubtreeStrat but for all others (anti / regular pattern) we can pre-calculate the requirement.
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        // TODO #229 LEFT and RIGHT paths are same as regular RandomReflectiveSubtreeStrat but for all others (anti / regular pattern) we can pre-calculate the requirement.
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        // don't do anything. Grafts don't require strategies
    }

    @Override
    public TruffleHierarchicalStrategy<T> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        this.rootStrategy = rootStrategy;
        return this;
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // TODO #229 implement by simulating create
        return requirementMap;
    }

    @Override
    public double minWeight(CreationInformation information) {
        return filterPossibilities(information).mapToDouble(x -> x.weight).min().orElse(Double.MAX_VALUE);
    }
}
