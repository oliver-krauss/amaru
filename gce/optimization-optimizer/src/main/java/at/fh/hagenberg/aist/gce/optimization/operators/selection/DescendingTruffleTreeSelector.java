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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Randomly selects a node in a tree
 *
 * @author Oliver Krauss on 21.11.2018
 */
public class DescendingTruffleTreeSelector extends ContractualTruffleTreeSelector {

    /**
     * Probability a node will be selected from the children instead of the current layer
     * 0 = the parent will always be selected (tree root)
     * 1 = a child will always be selected (tree leaves)
     */
    private double descentProbability = 0.7;

    private int maxDescent = Integer.MAX_VALUE;

    @Override
    public Node selectSubtree(Node tree) {
        Node nodeSelected = tree;
        List<Node> children = new ArrayList<>();
        nodeSelected.getChildren().forEach(children::add);
        int descended = 0;

        // select node for mutation
        boolean descend = RandomUtil.random.nextDouble() < descentProbability;
        // prevent from getting stuck with api nodes
        if (ExtendedNodeUtil.isAPINode(nodeSelected)) {
            descend = true;
        }
        while (maxDescent > descended && children.size() > 0 && descend) {
            descend = RandomUtil.random.nextDouble() < descentProbability;
            descended++;
            Node nextNode = children.get(RandomUtil.random.nextInt(children.size()));
            if (nextNode != null) {
                nodeSelected = nextNode;
                children.clear();
                nodeSelected.getChildren().forEach(children::add);
            } else {
                return nodeSelected;
            }
            if (ExtendedNodeUtil.isAPINode(nodeSelected)) {
                // prevent from getting stuck with api nodes
                descend = true;
            }
        }

        return nodeSelected;
    }

    @Override
    public Node selectSubtreeFromChoices(List<Node> choices) {
        return choices.isEmpty() ? null : selectSubtree(choices.get(RandomUtil.random.nextInt(choices.size())));
    }

    public void setDescentProbability(double descentProbability) {
        this.descentProbability = descentProbability;
    }

    public void setMaxDescent(int maxDescent) {
        this.maxDescent = maxDescent;
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        // Note: not publishing id, code, tests, originalSolution, bestKnownSolution

        Map<String, Descriptor> options = new HashMap<>();
        options.put("descentProbability", new Descriptor<>(descentProbability));
        options.put("maxDescent", new Descriptor<>(maxDescent));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "descentProbability":
                    setDescentProbability((Double) descriptor.getValue());
                    break;
                case "maxDescent":
                    setMaxDescent((Integer) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
