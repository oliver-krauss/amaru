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

import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * The truffle sequential strategy guarantees that the created objects will be made in sequence
 * If the sequence is finished, the next created object will be the same as the first object created
 *
 * @param <T> the object to be created (either a Node or also a terminal such as int, char, ...)
 */
public class TruffleSequentialStrategy<T> extends KnownValueStrategy<T> {

    protected Iterator<T> iterator;

    public TruffleSequentialStrategy(Collection<T> values) {
        super(values);
        this.iterator = values.iterator();
    }

    @Override
    public KnownValueStrategy<T> clone() {
        return new TruffleSequentialStrategy<>(new LinkedList<>(values));
    }

    /**
     * Checks if there is a next instance in the queue.
     * If there is not, then the next returned object will be the same as the first one from the queue
     *
     * @return if there is a next instance in the sequence that can be returned
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Resets the sequence so that {#current()} and {#next()} both will return the first object in the sequence
     */
    public void resetSequence() {
        iterator = values.iterator();
        current = null;
    }

    @Override
    public T next() {
        // cycle if necessary
        if (!hasNext()) {
            resetSequence();
        }

        current = iterator.next();
        return current;
    }
}
