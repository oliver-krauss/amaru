/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Oliver Krauss on 05.08.2020
 */
public final class RandomComparator<T> implements Comparator<T> {

    private final Map<T, Integer> map = new IdentityHashMap<>();
    private final Random random;

    public RandomComparator() {
        this(new Random());
    }

    public RandomComparator(Random random) {
        this.random = random;
    }

    @Override
    public int compare(T t1, T t2) {
        return Integer.compare(valueFor(t1), valueFor(t2));
    }

    private int valueFor(T t) {
        synchronized (map) {
            return map.computeIfAbsent(t, ignore -> random.nextInt());
        }
    }

}
