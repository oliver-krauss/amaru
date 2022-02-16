/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm;

import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface for SubgraphIterators needed by the Pattern Detection algorithms.
 *
 * @author Oliver Krauss on 12.12.2018
 */
public interface SubgraphIterator extends Iterator<PatternNodeWrapper> {

    /**
     * Returns the ID of the tree currently being processed.
     * @return id of root NodeWrapper
     */
    long getTreeId();

}