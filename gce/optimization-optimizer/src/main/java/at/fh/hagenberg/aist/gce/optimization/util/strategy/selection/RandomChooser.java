/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.selection;

import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Oliver Krauss on 21.11.2018
 */

public class RandomChooser<T> implements ChooseOption<T> {

    @Override
    public T choose(Collection<T> options) {
        int next = RandomUtil.random.nextInt(options.size());
        Iterator<T> iterator = options.iterator();
        for (int i = 0; i < next; i++) {
            iterator.next();
        }
        T value = iterator.next();
        return value;
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        return false;
    }
}
