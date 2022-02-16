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

import at.fh.hagenberg.machinelearning.core.Configurable;

import java.util.Collection;

/**
 * Selector interface for strategies that need to select from more than one option
 *
 * @author Oliver Krauss on 21.11.2018
 */
public interface ChooseOption<T> extends Configurable {

    /**
     * Choose one option out of many
     *
     * @param options to be chosen from
     * @return one item from the collection
     */
    public T choose(Collection<T> options);

}
