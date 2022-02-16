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
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionAnalyzer;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.RequirementInformation;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Invoke strategy was
 */
public class MinicCantInvokeStrategy extends RandomReflectiveSubtreeStrategy {

    public MinicCantInvokeStrategy(MinicContext minicContext, Class clazz) {
        super(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getClass(clazz),
                TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getOperators().entrySet().stream()
                        .filter(set -> set.getValue().contains(clazz)).map(Map.Entry::getKey).collect(Collectors.toList()),
                new HashMap<>(), null);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return null;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return null;
    }
}
