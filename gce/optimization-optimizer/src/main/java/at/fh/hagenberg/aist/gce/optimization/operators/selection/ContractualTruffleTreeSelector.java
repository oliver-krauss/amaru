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
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The contractual selector analyzes all nodes in a given tree according to the requirements in the CreationInformation
 * It then gives all VALID nodes to the real implementation for selection to the implementations criteria.
 *
 * @author Oliver Krauss on 09.01.2020
 */
public abstract class ContractualTruffleTreeSelector implements TruffleTreeSelector {

    private NodeWrapperWeightUtil util;

    // TODO #191 -> make the selector understand clazz, current depth
    // TODO #216 make the selector consider available data flow items
    @Override
    public Node selectSubtree(Node tree, CreationInformation info) {
        List<Node> choices = ExtendedNodeUtil.flatten(tree).filter(x -> !x.getClass().getName().startsWith("com.oracle.truffle.api")).collect(Collectors.toList());

        // serve the weight obligation
        double maxWeight = info.getConfiguration().getMaxWeight() - info.getCurrentWeight();
        if (info.getConfiguration().getMaxWeight() < Double.MAX_VALUE) {
            choices.removeIf(x -> util.weight(x) > maxWeight);
        }

        return selectSubtreeFromChoices(choices);
    }

    public abstract Node selectSubtreeFromChoices(List<Node> choices);

    @Required
    public void setUtil(NodeWrapperWeightUtil util) {
        this.util = util;
    }
}
