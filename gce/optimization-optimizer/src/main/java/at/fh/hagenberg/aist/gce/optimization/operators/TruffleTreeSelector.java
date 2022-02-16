/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.machinelearning.core.Configurable;
import com.oracle.truffle.api.nodes.Node;

/**
 * The truffle tree selector is responsible for choosing a part of a truffle tree using a specific strategy,
 * such as Random, ContextDriven, ...
 *
 * @author Oliver Krauss on 21.11.2018
 */
public interface TruffleTreeSelector extends Configurable {

    /**
     * Selects a node in the given tree (can be the root node as well) without constraints
     *
     * @param tree to select a node from
     * @return any node in the tree
     */
    Node selectSubtree(Node tree);

    /**
     * Selects a node in the given tree (can be the root node as well)
     *
     * @param tree to select a node from
     * @param info information the selection shall adhere to (such as depth, or weight)
     * @return any node in the tree
     */
    Node selectSubtree(Node tree, CreationInformation info);

    ;

}
