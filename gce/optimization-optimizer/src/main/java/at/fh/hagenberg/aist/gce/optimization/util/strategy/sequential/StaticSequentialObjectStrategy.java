/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.sequential;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;

/**
 * Special Strategy intended EXCLUSIVELY for the Truffle Node Sequence Creator.
 * WARNING: DO NOT USE THIS. Use {@link StaticObjectStrategy} instead
 */
public class StaticSequentialObjectStrategy extends TruffleSequentialStrategy<Object> {

    private Object object;

    private boolean next = true;

    public StaticSequentialObjectStrategy(Object object) {
        super(null); // We Override EVERYTHING
        this.object = object;
    }

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    public void resetSequence() {
        next = true;
    }

    @Override
    public Object current() {
        return next();
    }

    @Override
    public Object next() {
        next = false;
        return object;
    }
}