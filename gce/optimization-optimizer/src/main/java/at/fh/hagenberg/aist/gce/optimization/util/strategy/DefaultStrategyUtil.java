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

import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple helper class that returns the default strategies for simple data types
 * @author Oliver Krauss on 06.12.2019
 */
public class DefaultStrategyUtil {

    /**
     * Provides the default strategies in the system
     * @return all default strategies
     */
    public static Map<String, TruffleVerifyingStrategy> defaultStrategies() {
        // define subtree creation strategies
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("int", new KnownValueStrategy<>(new IntDefault().getValues()));
        strategies.put("char", new KnownValueStrategy<>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<>(new FloatDefault().getValues()));
        strategies.put("boolean", new KnownValueStrategy<>(new BooleanDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<>(new StringDefault().getValues()));
        return strategies;
    }

}
