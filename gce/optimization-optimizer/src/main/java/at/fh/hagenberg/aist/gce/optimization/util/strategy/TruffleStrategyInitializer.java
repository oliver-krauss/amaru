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

/**
 * The Truffle Strategy initializer is a helper class that enables creating truffle strategies in specific ways
 *
 * @param <T> the object to be created by the strategy
 */
public interface TruffleStrategyInitializer<T> {

    /**
     * Create a strategy to be used
     *
     * @return TruffleSimpleStrategy supporting T
     */
    public TruffleSimpleStrategy<T> createStrategy();

    /**
     * Creates a strategy from a specific subtree (ex. the problem description)
     *
     * @return TruffleSimpleStrategy supporting T
     */
    public TruffleSimpleStrategy<T> createStrategy(Node node);

    /**
     * Creates a strategy from a group of trees
     *
     * @return TruffleSimpleStrategy supporting T
     */
    public TruffleSimpleStrategy<T> createStrategy(Collection<Node> nodes);

}
