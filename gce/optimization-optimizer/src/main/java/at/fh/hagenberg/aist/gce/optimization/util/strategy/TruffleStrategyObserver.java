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

/**
 * The observable strategy enables changing strategies during runtime.
 * This enables strategies to react to additions or removals from strategies and allows them to self-repair on changes.
 * Every strategy that depends on another should observe all {@link TruffleObservableStrategy} they depend on.
 * <p>
 * Ex. if an anti-pattern is discovered and the search space restricted, all depending strategies that can't produce valid data anymore can restrict themselves.
 *
 * @author Oliver Krauss on 21.11.2018
 */
public interface TruffleStrategyObserver {

    /**
     * Event occurs when a {@link TruffleObservableStrategy} extends it's scope.
     * Ex. GraftingStrategy gets an additional graft
     */
    public void extended(TruffleObservableStrategy strategy);

    /**
     * Event occurs when a {@link TruffleObservableStrategy} restricts it's scope.
     * Ex. KnownValueStrategy removes one value.
     */
    public void restricted(TruffleObservableStrategy strategy);

    /**
     * Event occurs when the {@link TruffleObservableStrategy} becomes invalid and should be no longer used by the clients.
     */
    public void disabled(TruffleObservableStrategy strategy);

    /**
     * Event occurs when the {@link TruffleObservableStrategy} becomes valid again (ex. Empty KnownValueStrategy gains one value).
     *
     * @param strategy
     */
    public void enabled(TruffleObservableStrategy strategy);
}
