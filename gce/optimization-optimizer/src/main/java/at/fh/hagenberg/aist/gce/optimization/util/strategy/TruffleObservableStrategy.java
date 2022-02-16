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

import java.util.Collection;

/**
 * Interface for strategies that can be observed, so other strategies that depend upon them can react to changes in them.
 * Note: every strategy that can be changed should implement this interface or extend from {@link at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableStrategy}
 *
 * @author Oliver Krauss on 21.11.2018
 */
public interface TruffleObservableStrategy {

    /**
     * Allows registering to observe this strategy
     *
     * @param observer
     */
    public void register(TruffleStrategyObserver observer);

    /**
     * Allows unregistering from strategy
     *
     * @param observation
     */
    public void unregister(TruffleStrategyObserver observation);

    /**
     * Allows checking if this strategy can currently produce any valid result
     */
    public boolean isDisabled();

    /**
     * Returns all classes that are managed by this strategy
     */
    public Collection<Class> getManagedClasses();
}
