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
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.MinicReadArrayNode;
import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicCharArray;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicDoubleArray;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.RequirementInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Invoke strategy was
 */
public class MinicInvokeStrategy extends RandomReflectiveSubtreeStrategy {

    /**
     * Signatures allowed to be invoked.
     */
    private HashMap<MinicFunctionNode, TruffleFunctionSignature> signatures = new HashMap<>();

    private MinicFunctionNode function;

    private TruffleFunctionSignature signature;

    private Class invokeClazz;

    public MinicInvokeStrategy(MinicFunctionNode function) {
        super(fakeParameters(function),
                TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getOperators().entrySet().stream()
                        .filter(set -> set.getValue().contains(invokeFromBaseType(function.getType()))).map(Map.Entry::getKey).collect(Collectors.toList()),
                new HashMap<>(), null);
        this.function = function;
        this.invokeClazz = invokeFromBaseType(function.getType());
        this.signature = TruffleFunctionAnalyzer.getSignature(function.getCallTarget().getRootNode(), TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));
        this.overrideSelectorStrategy = true;
    }

    private static TruffleClassInformation fakeParameters(MinicFunctionNode function) {
        TruffleClassInformation tci = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID).getClass(invokeFromBaseType(function.getType()));
        tci = tci.copy(tci.getContext());

        // modify the call parameters
        TruffleFunctionSignature signature = TruffleFunctionAnalyzer.getSignature(function.getCallTarget().getRootNode(), TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));
        TruffleParameterInformation[] parameters = new TruffleParameterInformation[signature.size()];

        for (int i = 0; i < parameters.length; i++) {
            Class type = classFromType(signature.getArguments()[i]);
            TruffleParameterInformation fakeParam = new TruffleParameterInformation(type, "argument_" + i, null);
            parameters[i] = fakeParam;
        }

        // replace with our initializer
        TruffleClassInitializer originalInitializer = tci.getInitializers().get(0);
        tci.getInitializers().clear();
        tci.getInitializers().add(new InvocationInitializer(function, originalInitializer, parameters));

        return tci;
    }

    private static Class classFromType(String type) {
        Class value = null;
        if (type == null) {
            // happens if the type of the argument is indeterminable.
            return MinicExpressionNode.class;
        }
        switch (type) {
            case ("int"):
                value = MinicIntNode.class;
                break;
            case ("char"):
                value = MinicCharNode.class;
                break;
            case ("float"):
                value = MinicFloatNode.class;
                break;
            case ("double"):
                value = MinicDoubleNode.class;
                break;
            case ("object"):
                value = MinicExpressionNode.class;
                break;
            case ("string"):
                value = MinicStringNode.class;
                break;
            case ("array"):
                value = MinicReadArrayNode.MinicEntireArrayReadNode.class;
                break;
            default:
                throw new RuntimeException("Don't know this type " + type);
        }
        return value;
    }

    private static MinicBaseType baseTypefromClass(Class clazz) {
        if (MinicCharNode.class.isAssignableFrom(clazz)) {
            return MinicBaseType.CHAR;
        } else if (MinicIntNode.class.isAssignableFrom(clazz)) {
            return MinicBaseType.INT;
        } else if (MinicFloatNode.class.isAssignableFrom(clazz)) {
            return MinicBaseType.FLOAT;
        } else if (MinicDoubleNode.class.isAssignableFrom(clazz)) {
            return MinicBaseType.DOUBLE;
        } else if (MinicStringNode.class.isAssignableFrom(clazz)) {
            return MinicBaseType.STRING;
        } else if (MinicInvokeNode.MinicInvokeArrayNode.class.isAssignableFrom(clazz)) {
            return MinicBaseType.ARRAY;
        } else {
            return MinicBaseType.VOID;
        }
    }

    private static Class invokeFromBaseType(MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicInvokeNodeFactory.MinicInvokeCharNodeGen.class;
            case INT:
                return MinicInvokeNodeFactory.MinicInvokeIntNodeGen.class;
            case FLOAT:
                return MinicInvokeNodeFactory.MinicInvokeFloatNodeGen.class;
            case DOUBLE:
                return MinicInvokeNodeFactory.MinicInvokeDoubleNodeGen.class;
            case STRING:
                return MinicInvokeNodeFactory.MinicInvokeStringNodeGen.class;
            case VOID:
                return MinicInvokeNodeFactory.MinicInvokeVoidNodeGen.class;
            case ARRAY:
                return MinicInvokeNodeFactory.MinicInvokeArrayNodeGen.class;
            default:
                throw new AssertionError("Function type is unknown");
        }
    }

    public MinicFunctionNode getFunction() {
        return function;
    }

    public TruffleFunctionSignature getSignature() {
        return signature;
    }

    public Class getInvokeClazz() {
        return invokeClazz;
    }
}
