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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.Iterator;


public abstract class AbstractNodeEditor<N extends NodeWrapper> implements NodeEditor<N> {

    /**
     * Node to be modified currently
     */
    protected N node;

    public void edit(N node) {
        this.node = node;
    }
}
