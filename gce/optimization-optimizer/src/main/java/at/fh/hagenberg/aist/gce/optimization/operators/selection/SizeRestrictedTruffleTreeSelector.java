/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators.selection;

import at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Randomly selects a node in a tree with a maximal size
 *
 * @author Oliver Krauss on 21.11.2018
 */
public class SizeRestrictedTruffleTreeSelector extends ContractualTruffleTreeSelector {

    private int maxSize = 1;

    @Override
    public Node selectSubtree(Node tree) {
        List<Node> collect = ExtendedNodeUtil.flatten(tree).filter(this::decision).collect(Collectors.toList());
        return collect.get(RandomUtil.random.nextInt(collect.size()));
    }

    @Override
    public Node selectSubtreeFromChoices(List<Node> choices) {
        choices.removeIf(x -> !this.decision(x));
        return choices.get(RandomUtil.random.nextInt(choices.size()));
    }

    private boolean decision(Node x) {
        return !ExtendedNodeUtil.isAPINode(x) && ExtendedNodeUtil.size(x) <= maxSize;
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("maxSize", new Descriptor<>(maxSize));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "maxSize":
                    setMaxSize((Integer) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
}
