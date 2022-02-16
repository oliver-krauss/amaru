/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.executor;

import at.fh.hagenberg.aist.gce.optimization.util.ClassLoadingHelper;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Option for languages to modify the INPUT or OUTPUT so Java values are translated to language values
 */
public abstract class ValueModifier {

    private static Map<String, ValueModifier> languageRepositories = new HashMap<>();

    private static boolean loadedKnownClasses = false;

    public static ValueModifier loadForLanguage(String language) {
        if (!loadedKnownClasses) {
            // cheat so we auto load known classes of languages
            try {
                ValueModifier.register("c", (ValueModifier) ClassLoadingHelper.loadClassByName("at.fh.hagenberg.aist.gce.benchmark.MinicValueModifier").getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
            loadedKnownClasses = true;
        }

        return languageRepositories.getOrDefault(language, new ValueModifier() {

            @Override
            public Object[] toLanguage(Object[] input) {
                return input;
            }

            @Override
            public Object fromLanguage(Object result) {
                return result;
            }
        });
    }

    public static void register(String language, ValueModifier repository) {
        languageRepositories.put(language, repository);
    }

    /**
     * Transforms the input to inputs the language can work with
     * @param input to be transformed
     * @return valid inputs
     */
    public abstract Object[] toLanguage(Object[] input);

    /**
     * Transforms the output into something Java can work with
     * @param result to be transformed
     * @return java version of the result
     */
    public abstract Object fromLanguage(Object result);
}
