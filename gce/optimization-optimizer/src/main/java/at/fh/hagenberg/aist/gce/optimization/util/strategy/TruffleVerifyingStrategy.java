/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import com.oracle.truffle.api.nodes.Node;

import java.util.Collection;
import java.util.Map;

/**
 * @author Oliver Krauss on 06.05.2020
 */

public interface TruffleVerifyingStrategy<T> {

    /**
     * Creates a subtree with the given information
     *
     * @param information on what should be created, and what was already created
     * @return a subtree
     */
    T create(CreationInformation information);

    /**
     * Checks if the strategy is able to create something fitting the requested object
     *
     * @param information on the current state of the subtree being created
     * @return null if a creation is not possible with the given information; a RequirementSupport otherwise
     */
    RequirementInformation canCreate(CreationInformation information);

    /**
     * Enforce that hierarchical strategies must be able to reduce themselves to requirements
     * @param ast            to be mined for requirements
     * @param requirementMap map to enter requirements into / that they came from
     * @return               requirement map of ast
     */
    Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap);
}
