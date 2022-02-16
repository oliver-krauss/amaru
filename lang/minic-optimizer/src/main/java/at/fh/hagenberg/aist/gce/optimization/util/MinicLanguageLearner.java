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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.DefaultStrategyUtil;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleSimpleStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oliver Krauss on 04.12.2019
 */

public class MinicLanguageLearner extends TruffleLanguageLearner {

    public MinicLanguageLearner(TruffleLanguageInformation information) {
        super(information);
    }

    @Override
    protected Map<String, TruffleVerifyingStrategy> getStrategies() {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<>(MinicLanguage.INSTANCE.getContextReference().get()));
        return strategies;
    }

    @Override
    protected Map<String, TruffleVerifyingStrategy> getSampleInput() {
        Map<String, TruffleVerifyingStrategy> strategies = DefaultStrategyUtil.defaultStrategies();
        strategies.put(MinicCharArray.class.getName(), new KnownValueStrategy<>(Collections.singleton(new MinicCharArray(new int[]{4}))));
        strategies.put(MinicIntArray.class.getName(), new KnownValueStrategy<>(Collections.singleton(new MinicIntArray(new int[]{4}))));
        strategies.put(MinicFloatArray.class.getName(), new KnownValueStrategy<>(Collections.singleton(new MinicFloatArray(new int[]{4}))));
        strategies.put(MinicDoubleArray.class.getName(), new KnownValueStrategy<>(Collections.singleton(new MinicDoubleArray(new int[]{4}))));
        strategies.put(MinicStringArray.class.getName(), new KnownValueStrategy<>(Collections.singleton(new MinicStringArray(new int[]{4}))));
        return strategies;
    }

    @Override
    protected String getMinimalProgram() {
        return "" +
            "int globalI;" +
            "" +
            "void weight(){" +
            "int i;" +
            "} " +
            "int main() {" +
            "globalI = 1;" +
            "weight(); " +
            "return 0;}";
    }

    @Override
    protected boolean returnValueInvalid(Object returnValue) {
        return returnValue != null;
    }


}
