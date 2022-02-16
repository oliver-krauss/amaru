/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm.editor;

import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.Iterator;

/**
 * A Node editor is a utility for the pattern mining that manipulates the content of a node
 * before being processed by the pattern mining. This is necessary to abstract irrelevant information,
 * or to mine for specific details.
 *
 * For example it should be used to label variables across trees, or to remove / keep specific values.
 *
 * The editor guarantees that it only returnes each permutation of a Node ONCE.
 *
 * @param <N> Node type that will be used as in / output
 */
public interface NodeEditor<N extends NodeWrapper> extends Iterator<N> {

    /**
     * This sets what the iterator is supposed to iterate over.
     *
     * @param node to be iterated over
     */
    void edit(N node);
}
