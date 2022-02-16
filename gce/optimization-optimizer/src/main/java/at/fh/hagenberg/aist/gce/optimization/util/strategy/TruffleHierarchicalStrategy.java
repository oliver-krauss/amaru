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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;
import java.util.Map;

/**
 * A strategy defines how truffle values (both nodes as well as terminal values!) will be created
 * These include for example int, char, java.lang.String
 *
 * @param <T> the object to be created (either a Node or also a terminal such as int, char, ...)
 */
public interface TruffleHierarchicalStrategy<T> extends TruffleVerifyingStrategy<T> {

    /**
     * Returns all classes that are managed by this strategy
     */
    Collection<Class> getManagedClasses();

    /**
     * Returns the minimal weight that can be created
     * NOTE: if we ever need something like this again, remove this function and return something more complex
     * in canCreate;
     *
     * @param information that shall be created
     * @return minimal weight that can be created.
     */
    double minWeight(CreationInformation information);

    /**
     * Checks if the strategy has the necessery sub-strategies to create a requested object
     * Goes through all parameters, and is more expensive than canCreate.
     *
     * @param information on the current state of the subtree being created
     * @return null if a creation is not possible with the given information; a RequirementSupport otherwise
     */
    RequirementInformation canCreateVerbose(CreationInformation information);

    /**
     * Adds a requirement to the strategy that must be checked upon creation of a new node (and also canCreate obviously).
     * Only strategies that implement real valued data MUST implement this (e.g. {@link at.fh.hagenberg.aist.gce.optimization.util.strategy.random.RandomReflectiveSubtreeStrategy} but not {@link TruffleMasterStrategy}
     *
     * @param requirement to be injected
     */
    void addCreateRequirement(Requirement requirement);

    /**
     * Removes a requirement from the strategy that doesn't need to be checked anymore
     * Only strategies that implement real valued data MUST implement this (e.g. {@link at.fh.hagenberg.aist.gce.optimization.util.strategy.random.RandomReflectiveSubtreeStrategy} but not {@link TruffleMasterStrategy}
     * @param requirement
     */
    void removeCreateRequirement(Requirement requirement);

    /**
     * Allows a strategy to attach itself to the given strategy (e.g. inject it into the context)
     * Used primarily to tell specialized strategies about the master strategy they are being attached into.
     * @param strategy
     */
    void attach(TruffleHierarchicalStrategy strategy);



    /**
     * Allows injecting an "owner" master strategy that contains all the pattern information for our knowledge graph
     * @param rootStrategy
     */
    TruffleHierarchicalStrategy<T> injectRootStrategy(TruffleMasterStrategy rootStrategy);
}
