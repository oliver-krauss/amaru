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

import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;
import at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Randomly selects a node in a tree
 *
 * @author Oliver Krauss on 21.11.2018
 */
public class RandomTruffleTreeSelector extends ContractualTruffleTreeSelector {

    @Override
    public Node selectSubtree(Node tree) {
        List<Node> collect = ExtendedNodeUtil.flatten(tree).filter(x -> !ExtendedNodeUtil.isAPINode(x)).collect(Collectors.toList());
        return collect.get(RandomUtil.random.nextInt(collect.size()));
    }

    @Override
    public Node selectSubtreeFromChoices(List<Node> choices) {
        if (choices.size() < 1) {
            return null;
        }
        return choices.get(RandomUtil.random.nextInt(choices.size()));
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        // Note: not publishing id, code, tests, originalSolution, bestKnownSolution

        Map<String, Descriptor> options = new HashMap<>();
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        // we have no options
        return false;
    }
}
