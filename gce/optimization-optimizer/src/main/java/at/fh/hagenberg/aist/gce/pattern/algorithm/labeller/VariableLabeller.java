/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm.labeller;

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

/**
 * Extracts the variables from different node types.
 * Note that we only support one variable per node!
 */
public interface VariableLabeller {

    /**
     * Extracts the variable name that the node wrapper is accessing
     * @param wrapper to be extracted
     * @return variable name or null
     */
    String label(NodeWrapper wrapper);

    /**
     * Injects the variable name into a given node wrapper to return previously extracted variables
     * @param wrapper      to be injected into
     * @param variableName to be injected
     */
    void inject(NodeWrapper wrapper, String variableName);
}
