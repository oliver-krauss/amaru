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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.ByteArrayOutputStream;

/**
 * Default executor that runs optimizations internally
 *
 * @author Oliver Krauss on 28.10.2019
 */
public class InternalExecutor extends AbstractExecutor {

    public InternalExecutor(String languageId, String code, String entryPoint, String function) {
        super(languageId, code, entryPoint, function);
        modifier = ValueModifier.loadForLanguage(this.languageId);
    }

    /**
     * Modifier for input and output
     * Only the internal executor needs this as all other implementations just call on it.
     */
    private ValueModifier modifier;

    @Override
    public ExecutionResult conductTest(Node node, Object[] input) {
        // replace current node with node to be tested via the parent (so the exchange really happens everytime)
        origin.getParent().getChildren().iterator().next().replace(node);
        // TODO #254 IF the function calls itself it will call the OLD code

        // modify the input
        input = modifier.toLanguage(input);

        Throwable e = null;
        long[] performance = new long[repeats];

        // run the test
        try {
            // run the node
            Object result = null;
            for (int i = 0; i < repeats; i++) {
                out.reset();
                long start = System.nanoTime();
                //ctx.enter();
                result = input == null ?
                    main.call() :
                    main.call(input);
                //ctx.leave();
                long end = System.nanoTime();
                performance[i] = end - start;
            }

            result = modifier.fromLanguage(result);
            return new ExecutionResult(result, out.toString(), performance, true);
        } catch (Exception | Error ex) {
            e = ex;
        }
        return new ExecutionResult(e, out.toString(), performance, false);
    }
}
