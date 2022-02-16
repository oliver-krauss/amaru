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

import com.oracle.truffle.api.nodes.Node;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * Simply executes some code without doing anything.
 * Kept here to show how Truffle is "usually" used.
 * @author Oliver Krauss on 29.10.2019
 */
public class DefaultExecutor implements Executor {

    private Object x;

    public void execute(String id, String source) {
        Context ctx = Context.newBuilder().build();
        ctx.initialize(id);
        Value eval = ctx.eval(Source.create(id, source));
        int i = 0;
    }


    public static void main(String[] args) {
        new DefaultExecutor().execute("", "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}");
    }

    @Override
    public ExecutionResult test(Node node, Object[] input) {
        return null;
    }

    @Override
    public Executor replace(String language, String code, String entryPoint, String function) {
        return this;
    }
}
