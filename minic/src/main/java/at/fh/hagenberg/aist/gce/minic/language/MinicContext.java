/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.language;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.*;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.parser.Parser;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;

import java.io.BufferedReader;
import java.io.PrintWriter;

/**
 * Context for MinicLanguage execution
 * Created by Oliver Krauss on 13.05.2016.
 */
public class MinicContext {
    private static final Layout LAYOUT = Layout.createLayout();

    private final BufferedReader input;
    private final PrintWriter output;
    private final MinicFunctionRegistry functionRegistry;
    private final Shape emptyShape;
    private final TruffleLanguage.Env env;
    private MaterializedFrame globalStorage;

    public MinicContext(TruffleLanguage.Env env, BufferedReader input, PrintWriter output) {
        this(env, input, output, true);
    }

    public MinicContext() {
        this(null, null, null, false);
    }

    private MinicContext(TruffleLanguage.Env env, BufferedReader input, PrintWriter output, boolean installBuiltins) {
        this.input = input;
        this.output = output;
        this.env = env;
        this.functionRegistry = new MinicFunctionRegistry();
        installBuiltins(installBuiltins);

        this.emptyShape = LAYOUT.createShape(new MinicObjectType());
    }

    public PrintWriter getOutput() {
        return output;
    }

    public BufferedReader getInput() {
        return input;
    }

    /**
     * Adds all builtin functions to the {@link MinicFunctionRegistry}. This method lists all
     * {@link MinicBuiltinNode} implementation classes.
     */
    private void installBuiltins(boolean registerRootNodes) {
        installArgBuiltin(PrintNodeFactory.getInstance(), registerRootNodes);
        installBuiltin(ReadNodeFactory.getInstance(), registerRootNodes);
        installArgBuiltin(MathExponentialNodeFactory.getInstance(), registerRootNodes);
        installArgBuiltin(MathSqrtNodeFactory.getInstance(), registerRootNodes);
        installArgBuiltin(MathPowfNodeFactory.getInstance(), registerRootNodes);
        installBuiltin(RandomNodeFactory.getInstance(), registerRootNodes);
        installArgBuiltin(LengthNodeFactory.getInstance(), registerRootNodes);
    }

    public void installBuiltin(NodeFactory<? extends MinicBuiltinNode> factory, boolean registerRootNodes) {
        // Prepare Arguments
        int argumentCount = factory.getExecutionSignature().size();

        /* Instantiate the builtin node. This node performs the actual functionality. */
        MinicBuiltinNode builtinBodyNode = factory.createNode(this);
        /* The name of the builtin function is specified via an annotation on the node class. */
        String name = lookupNodeInfo(builtinBodyNode.getClass()).shortName();

        /* Wrap the builtin in a RootNode. Truffle requires all AST to start with a RootNode. */
        MinicRootNode rootNode = new MinicRootNode(MinicLanguage.INSTANCE, this, new FrameDescriptor(), builtinBodyNode, name);

        if (registerRootNodes) {
            /* Register the builtin function in our function registry. */
            getFunctionRegistry().register(name, rootNode, builtinBodyNode.getType());
        } else {
            // make sure the function is known
            getFunctionRegistry().lookup(name);
        }
    }

    public void installArgBuiltin(NodeFactory<? extends MinicBuiltinNode> factory, boolean registerRootNodes) {
        // Prepare Arguments
        int argumentCount = factory.getExecutionSignature().size();
        MinicExpressionNode[] argumentNodes = new MinicExpressionNode[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            argumentNodes[i] = MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.create(i);
        }

        /* Instantiate the builtin node. This node performs the actual functionality. */
        MinicBuiltinNode builtinBodyNode = factory.createNode(argumentNodes, this);
        /* The name of the builtin function is specified via an annotation on the node class. */
        String name = lookupNodeInfo(builtinBodyNode.getClass()).shortName();

        /* Wrap the builtin in a RootNode. Truffle requires all AST to start with a RootNode. */
        MinicRootNode rootNode = new MinicRootNode(MinicLanguage.INSTANCE, this, new FrameDescriptor(), builtinBodyNode, name);

        if (registerRootNodes) {
            /* Register the builtin function in our function registry. */
            getFunctionRegistry().register(name, rootNode, builtinBodyNode.getType());
        } else {
            // make sure the function is known
            getFunctionRegistry().lookup(name);
        }
    }


    public static NodeInfo lookupNodeInfo(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        NodeInfo info = clazz.getAnnotation(NodeInfo.class);
        if (info != null) {
            return info;
        } else {
            return lookupNodeInfo(clazz.getSuperclass());
        }
    }

    public static boolean isMinicObject(TruffleObject value) {
        return value instanceof DynamicObject && isMinicObject((DynamicObject) value);
    }

    public static boolean isMinicObject(DynamicObject value) {
        return value.getShape().getObjectType() instanceof MinicObjectType;
    }

    /**
     * Returns the registry of all functions that are currently defined.
     */
    public MinicFunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }


    /**
     * Evaluate a source, causing any definitions to be registered (but not executed).
     *
     * @param source The {@link Source} to parse.
     */
    public void evalSource(Source source) {
        Parser.parseMinic(this, source);
    }

    public static Object fromForeignValue(Object a) {
        if (a instanceof Character || a instanceof Number || a instanceof String) {
            return a;
        } else if (a instanceof TruffleObject) {
            return a;
        } else if (a instanceof MinicContext) {
            return a;
        }
        throw new IllegalStateException(a + " is not a Truffle value");
    }

    public MaterializedFrame getGlobalStorage() {
        return globalStorage;
    }

    public void setGlobalStorage(MaterializedFrame globalStorage) {
        this.globalStorage = globalStorage;
    }
}
