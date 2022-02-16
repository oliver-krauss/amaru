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
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Randomly selects a node in a tree that is of maximal depth and width
 *
 * @author Oliver Krauss on 21.11.2018
 */
public class DepthWidthRestrictedTruffleTreeSelector extends ContractualTruffleTreeSelector {

    private int maxDepth = 1;

    private int maxWidth = -1;

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
        return !ExtendedNodeUtil.isAPINode(x)
            && (maxDepth <= 0 || maxDepth >= ExtendedNodeUtil.maxDepth(x))
            && (maxWidth <= 0 || maxWidth >= ExtendedNodeUtil.maxWidth(x));
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("maxDepth", new Descriptor<>(maxDepth));
        options.put("maxWidth", new Descriptor<>(maxWidth));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "maxDepth":
                    setMaxDepth((Integer) descriptor.getValue());
                    break;
                case "maxWidth":
                    setMaxWidth((Integer) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }
}
