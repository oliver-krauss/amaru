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

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicEvalRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNull;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.MinicBuiltinNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.nodes.GraphPrintVisitor;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Language entry point for truffle
 * Created by Oliver Krauss on 13.05.2016.
 */
@TruffleLanguage.Registration(id = MinicLanguage.ID, name = "Mini ANSI C11", version = "0.0", mimeType = {MinicLanguage.MINIC_IR_MIME_TYPE})
public class MinicLanguage extends TruffleLanguage<MinicContext> {

    public static MinicLanguage INSTANCE; // TODO #53 MinicLang should be managed by polyglot engine

    public static final String MINIC_IR_MIME_TYPE = "application/x-c";
    public static final String ID = "c";

    // TODO #53 Remove. We are just using it to get the context out of the polyglot engine in the run method
    private static MinicContext currentContext;

    private static List<NodeFactory<? extends MinicBuiltinNode>> builtins = Collections.synchronizedList(new ArrayList<>());

    public MinicLanguage() {
        INSTANCE = this;
    }

    @Override
    protected MinicContext createContext(Env env) {
        final BufferedReader in = new BufferedReader(new InputStreamReader(env.in()));
        final PrintWriter out = new PrintWriter(env.out(), true);
        MinicContext context = new MinicContext(env, in, out);
        for (NodeFactory<? extends MinicBuiltinNode> builtin : builtins) {
            context.installBuiltin(builtin, true);
        }
        currentContext = context;
        return context;
    }

    @Override
    protected void initializeContext(MinicContext context) throws Exception {
        // nothing to do here
    }

    @Override
    protected boolean patchContext(MinicContext context, Env newEnv) {
        return true;
    }

    @Override
    protected void disposeContext(MinicContext context) {
        CompilerAsserts.neverPartOfCompilation();
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        return parse(request.getSource());
    }

    public CallTarget parse(com.oracle.truffle.api.source.Source code) {
        final MinicContext c = currentContext;
        final Exception[] failed = {null};
        try {
            c.evalSource(code);
            failed[0] = null;
        } catch (Exception e) {
            failed[0] = e;
        }
        RootCallTarget main = c.getFunctionRegistry().lookup("main").getCallTarget();
        RootNode evalMain;
        if (main != null) {
            /*
             * We have a main function, so "evaluating" the parsed source means invoking that main
             * function. However, we need to lazily register functions into the SLContext first, so
             * we cannot use the original SLRootNode for the main function. Instead, we create a new
             * SLEvalRootNode that does everything we need.
             */
            evalMain = new MinicEvalRootNode(this, main);
        } else {
            /*
             * Even without a main function, "evaluating" the parsed source needs to register the
             * functions into the SLContext.
             */
            evalMain = new MinicEvalRootNode(this, null);
        }
        MinicLanguage.INSTANCE = this;
        return INSTANCE.lastParsed = lastParsed = Truffle.getRuntime().createCallTarget(evalMain);
    }

    private CallTarget lastParsed;

    /**
     * Helper function because Truffle changed the API
     *
     * @return
     */
    public CallTarget getLastParsed() {
        return lastParsed;
    }

    @Override
    protected Object findExportedSymbol(MinicContext context, String globalName, boolean onlyExplicit) {
        for (MinicFunctionNode f : context.getFunctionRegistry().getFunctions()) {
            if (globalName.equals(f.getName())) {
                return f;
            }
        }
        return null;
    }

    @Override
    protected boolean isVisible(MinicContext context, Object value) {
        return value != MinicNull.SINGLETON;
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof MinicFunctionNode;
    }


    public static long run(Context context, Path path, PrintWriter logOutput, PrintWriter out, int repeats, List<NodeFactory<? extends MinicBuiltinNode>> currentBuiltins) throws IOException {
        return run(context, path, logOutput, out, repeats, currentBuiltins, null);
    }

    public static long run(Context context, Path path, PrintWriter logOutput, PrintWriter out, int repeats, List<NodeFactory<? extends MinicBuiltinNode>> currentBuiltins, String executionIdentifier) throws IOException {
        builtins = currentBuiltins;

        if (logOutput != null) {
            logOutput.println("== running on " + Truffle.getRuntime().getName());
            // logOutput.println("Source = " + source.getCode());
        }

        Source src = Source.newBuilder(MinicLanguage.ID, path.toFile()).build();
        printScript("before execution", currentContext, logOutput, executionIdentifier);
        long totalRuntime = 0;
        try {
            long start = System.nanoTime();
            for (int i = 0; i < repeats; i++) {
                /* Call the main entry point, without any arguments. */
                try {
                    Object result = context.eval(src);
                    if (result != null) {
                        out.println(result);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    out.println(ex);
                }
                if (logOutput != null && repeats > 1 && ((i + 1) % 1000) == 0) {
                    // we only care about 1000 executions to get a better average
                    long end = System.nanoTime();
                    totalRuntime += end - start;
                    logOutput.println("== iteration " + (i + 1) + ": " + ((end - start) / 1000000) + " ms");
                    start = System.nanoTime();
                }
            }
        } finally {
            if (logOutput != null) {
                logOutput.println("total runtime = " + (totalRuntime / 1000000) + " ms");
                logOutput.println("average runtime = " + (totalRuntime / 1000000) / repeats + " ms");
            }
            printScript("after execution", currentContext, logOutput, executionIdentifier);
        }
        return totalRuntime;
    }

    /* Change to true if you want to see the AST on the console. */
    private static boolean printASTToLog = false;
    /* Change to true if you want to see source attribution for the AST to the console */
    private static boolean printSourceAttributionToLog = false;
    /* Change to dump the AST to IGV over the network. */
    private static boolean dumpASTToIGV = false;

    public static void setPrintASTToLog(boolean printASTToLog) {
        MinicLanguage.printASTToLog = printASTToLog;
    }

    public static void setPrintSourceAttributionToLog(boolean printSourceAttributionToLog) {
        MinicLanguage.printSourceAttributionToLog = printSourceAttributionToLog;
    }

    public static void setDumpASTToIGV(boolean dumpASTToIGV) {
        MinicLanguage.dumpASTToIGV = dumpASTToIGV;
    }

    private static void printScript(String groupName, MinicContext context, PrintWriter logOutput, String executionIdentifier) {
        if (dumpASTToIGV) {
            GraphPrintVisitor graphPrinter = new GraphPrintVisitor();
            graphPrinter.beginGroup((executionIdentifier != null ? executionIdentifier : "") + " " + groupName);
            for (MinicFunctionNode function : context.getFunctionRegistry().getFunctions()) {
                RootCallTarget callTarget = function.getCallTarget();
                if (callTarget != null) {
                    graphPrinter.beginGraph(function.toString()).visit(callTarget.getRootNode());
                }
            }
            graphPrinter.printToNetwork(true);
        }
        if (printASTToLog && logOutput != null) {
            logOutput.println("--- " + (executionIdentifier != null ? executionIdentifier : "") + " " + groupName + " ---");
            for (MinicFunctionNode function : context.getFunctionRegistry().getFunctions()) {
                RootCallTarget callTarget = function.getCallTarget();
                if (callTarget != null) {
                    logOutput.println("=== " + function);
                    NodeUtil.printTree(logOutput, callTarget.getRootNode());
                }
            }
        }
        if (printSourceAttributionToLog && logOutput != null) {
            logOutput.println("--- " + (executionIdentifier != null ? executionIdentifier : "") + " " + groupName + " ---");
            for (MinicFunctionNode function : context.getFunctionRegistry().getFunctions()) {
                RootCallTarget callTarget = function.getCallTarget();
                if (callTarget != null) {
                    logOutput.println("=== " + function);
                    NodeUtil.printSourceAttributionTree(logOutput, callTarget.getRootNode());
                }
            }
        }
    }

    @Override
    protected Iterable<Scope> findTopScopes(MinicContext context) {
        return null;
    }

    public static MinicContext getNewCurrentContext() { // TODO #53 replace with getCurrentContext
        return getCurrentContext(MinicLanguage.class);
    }

    public static MinicContext getCurrentContext() {
        return currentContext;
    }

}
