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

import at.fh.hagenberg.aist.gce.optimization.language.Accessor;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.*;

/**
 * Base class for executors
 *
 * @author Oliver Krauss on 28.10.2019
 */
public abstract class AbstractExecutor implements Executor {

    public static final String ACCESSOR_PREFIX = "access-";

    /**
     * Global Frame for accessing the heap (can be null!)
     */
    private MaterializedFrame globalScope;

    /**
     * Id of language this executor is running in
     */
    protected String languageId;

    /**
     * Unpased code
     */
    protected String code;

    /**
     * Name of function to call for optimization testing
     */
    protected String entryPoint;

    /**
     * Name of function to optimize
     */
    protected String function;

    /**
     * Root function node of node to be tested
     */
    protected RootNode root;

    /**
     * Node to be tested
     */
    protected Node origin;

    /**
     * The Function call target that will be run
     * Note that you can elect to run the entire code (main == code main method)
     * or alternatively just one function (main == function call node)
     */
    protected CallTarget main;

    /**
     * Stream where the console output of the program is written
     */
    protected ByteArrayOutputStream out = new ByteArrayOutputStream();

    /**
     * Amount of times the test is to be repeated
     */
    protected int repeats = 1;

    /**
     * Context we created the program with
     */
    protected Context ctx;

    /**
     * Timeout in milliseconds.
     * -1 means "as long as it takes"
     */
    protected long timeout = -1;

    /**
     * The Executor will initialize an accessor for the language we want to use
     *
     * @param languageId Language you want to optimize with
     * @param code       that will be parsed
     * @param entryPoint function that will be called (can be equal to function but doesn't have to be)
     * @param function   that will be selected in root and origin
     */
    public AbstractExecutor(String languageId, String code, String entryPoint, String function) {
        init(languageId, code, entryPoint, function);
    }

    protected void init(String languageId, String code, String entryPoint, String function) {
        this.languageId = languageId;
        this.code = code;
        this.entryPoint = entryPoint;
        this.function = function;

        // enter or create context
        ctx = Context.newBuilder().out(out).build(); // .allowExperimentalOptions(true)
        ctx.initialize(ACCESSOR_PREFIX + this.languageId);
        try {
            Source src = Source.create(ACCESSOR_PREFIX + this.languageId, code);
            ctx.enter();
            ctx.eval(src);
        } catch (Exception e) {
            // we don't care. The execution may fail, but the code is still parsed.
            System.out.println("WARNING: Context evaluation has failed. You might want to check this: " + e.getMessage());
        }
        Accessor access = Accessor.getAccessor(ACCESSOR_PREFIX + this.languageId);
        this.main = access.getCallTarget(entryPoint);
        this.root = access.getRootNode(function);
        this.origin = access.getNodeToOptimize(root);
        this.globalScope = access.getGlobalScope();
    }

    public RootNode getRoot() {
        return root;
    }

    public Node getOrigin() {
        return origin;
    }

    public CallTarget getMain() {
        return main;
    }

    public ByteArrayOutputStream getOut() {
        return out;
    }

    public int getRepeats() {
        return repeats;
    }

    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    public MaterializedFrame getGlobalScope() {
        return globalScope;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Executor that actually conducts the test
     */
    protected ExecutorService service = Executors.newSingleThreadExecutor();

    /**
     * Test is executed with a TIMEOUT if that timeout is exceeded it will automatically return a failure.
     * The timeout is set in the {@link #timeout} field. -1 means it will run indefinitely long
     *
     * @param node  to be run in context
     * @param input for the main function!
     * @return execution result
     */
    @Override
    public ExecutionResult test(Node node, Object[] input) {
        if (timeout < 0) {
            return conductTest(node, input);
        }
        Throwable e = null;
        try {
            return service.submit(() -> conductTest(node, input)).get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            // kill the thread and restart the service
            service.shutdownNow();
            service = Executors.newSingleThreadExecutor();
            cleanup();

            // log the exception
            e = ex;
            System.out.println("Execution Timeout");
            System.out.println(" Heap " + DecimalFormat.getNumberInstance().format(Runtime.getRuntime().freeMemory()));

            if (!(e instanceof TimeoutException)) {
                e.printStackTrace();
            }
        }
        return new ExecutionResult(e, out.toString(), new long[repeats], false);
    }

    /**
     * Option for implementations to shut down services, etc. after an interrupt has been thrown.
     */
    protected void cleanup() {

    }

    /**
     * Actual testing implementation. Does not have to consider timeout issues
     */
    public abstract ExecutionResult conductTest(Node node, Object[] input);

    @Override
    public Executor replace(String language, String code, String entryPoint, String function) {
        init(language, code, entryPoint, function);
        return this;
    }
}
