/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.random;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy hardcoded to produce ONLY ASTS
 * ONLY FOR PATTERN VERIFICATION.
 */
public class MinicAntiFunctionLiteralStrategy extends RandomReflectiveSubtreeStrategy {

    public MinicAntiFunctionLiteralStrategy(MinicContext minicContext) {
        super(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getClass(MinicFunctionLiteralNode.class),
                TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getOperators().entrySet().stream()
                        .filter(set -> set.getValue().contains(MinicFunctionLiteralNode.class)).map(Map.Entry::getKey).collect(Collectors.toList()),
                loadTerminalStrategy(minicContext), null);
    }

    private static Map<String, TruffleVerifyingStrategy> loadTerminalStrategy(MinicContext minicContext) {
        HashMap<String, TruffleVerifyingStrategy> terminals = new HashMap<>();
        // exclude read (as read from commandline), and the _benchmark and _entry functions, as they should not exist in context
        List<String> functionNames = new ArrayList<>();
        functionNames.add("I DO NOT EXIST");
        terminals.put("java.lang.String", new KnownValueStrategy(functionNames));
        return terminals;
    }
}
