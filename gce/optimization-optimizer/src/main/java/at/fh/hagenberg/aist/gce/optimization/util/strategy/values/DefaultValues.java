/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.values;

import java.util.Collection;

/**
 * Helper class that contains default values for often used classes
 *
 * @param <T>
 */
public class DefaultValues<T> {

    /**
     * List of values
     */
    protected Collection<T> values;

    /**
     * Creates the default values by constructor
     *
     * @param values
     */
    public DefaultValues(Collection<T> values) {
        this.values = values;
    }

    public Collection<T> getValues() {
        return values;
    }
}
