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

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInitializer;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleParameterInformation;

public class InvocationInitializer extends at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInitializer {

    private MinicFunctionNode node;

    public InvocationInitializer(MinicFunctionNode node, TruffleClassInitializer originalInitializer, TruffleParameterInformation[] parameters) {
        isMethod = true;
        this.clazz = originalInitializer.clazz;
        this.createMethod = originalInitializer.createMethod;
        this.parameters = parameters;
        this.node = node;
    }

    @Override
    public Object instantiate(Object[] parameters) {
        try {
            Object[] realParameters = new Object[2];
            MinicExpressionNode[] exprArray = new MinicExpressionNode[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                exprArray[i] = (MinicExpressionNode) parameters[i];
            }
            realParameters[0] = exprArray;
            realParameters[1] = new MinicFunctionLiteralNode(node.getName());
            return isMethod ? createMethod.invoke(null, realParameters) : createConstructor.newInstance(realParameters);
        } catch (Exception e) {
            System.out.println("Instantiation of " + this.getClazz() + " failed.");
            System.out.println("param " + (parameters == null ? "null" : parameters.length));
            if (parameters != null){
                for (Object parameter : parameters) {
                    System.out.println(parameter == null ? "null" : parameter.getClass().getName() + " " + parameter);
                }
            }
            e.printStackTrace();
        }
        return super.instantiate(parameters);
    }
}
