/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.lang.optimization.js;

import at.fh.hagenberg.aist.gce.optimization.language.JavaScriptAccessor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.IOException;

/**
 * @author Oliver Krauss on 26.12.2018
 */
public class JavaScriptOptimizationTest {

    public static void main(String[] args) throws IOException {
        //System.in.read();

        // NOTICE: Do not use classes here that Graal is supposed to load. Otherwise we will get inconsistency errors later .

        Context ctx = Context.newBuilder().build();
        ctx.initialize(JavaScriptAccessor.ID);
        ctx.eval(Source.create(JavaScriptAccessor.ID, "int main() {print(3 + 5);return 0;}"));
    }
}
