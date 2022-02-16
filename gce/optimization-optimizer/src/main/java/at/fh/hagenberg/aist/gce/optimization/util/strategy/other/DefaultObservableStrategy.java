/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.other;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleObservableStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleStrategyObserver;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Oliver Krauss on 21.11.2018
 */

public abstract class DefaultObservableStrategy implements TruffleObservableStrategy {

    protected List<TruffleStrategyObserver> observers = new LinkedList<>();

    protected boolean disabled = false;

    @Override
    public void register(TruffleStrategyObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregister(TruffleStrategyObserver observer) {
        observers.remove(observer);
    }

    protected void notifyExtension() {
        observers.forEach(x -> x.extended(this));
    }

    protected void notifyRestriction() {
        observers.forEach(x -> x.restricted(this));
    }

    protected void notifyDisable() {
        if (!disabled) {
            disabled = true;
            observers.forEach(x -> x.disabled(this));
        }
    }

    protected void notifyEnable() {
        if (disabled) {
            disabled = false;
            observers.forEach(x -> x.enabled(this));
        }
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }
}
