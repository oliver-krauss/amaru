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
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Randomly selects a node in a tree with a maximum complexity measure
 *
 * @author Oliver Krauss on 21.11.2018
 */
public class CodeComplexityTruffleTreeSelector extends ContractualTruffleTreeSelector {

    /**
     * Maximum allowed complexity (can be 0!)
     */
    private int maxComplexity = 1;

    /**
     * Language that the complexity will be measured by
     */
    private String language;

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
        return !ExtendedNodeUtil.isAPINode(x) && maxComplexity >= NodeWrapper.cyclomaticComplexity(x, language);
    }


    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("maxComplexity", new Descriptor<>(maxComplexity));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "maxComplexity":
                    setMaxComplexity((Integer) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public int getMaxComplexity() {
        return maxComplexity;
    }

    public void setMaxComplexity(int maxComplexity) {
        this.maxComplexity = maxComplexity;
    }

    public String getLanguage() {
        return language;
    }

    @Required
    public void setLanguage(String language) {
        this.language = language;
    }

}
