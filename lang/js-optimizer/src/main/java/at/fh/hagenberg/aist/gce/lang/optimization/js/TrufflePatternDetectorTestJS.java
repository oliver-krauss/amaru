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

import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import com.oracle.truffle.js.builtins.DataViewPrototypeBuiltinsFactory;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.GetViewValueNode;
import org.graalvm.polyglot.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class so I can load the MINIC classes into the classpath
 *
 * @author Oliver Krauss on 28.11.2018
 */

public class TrufflePatternDetectorTestJS {

    public static void main(String[] args) throws IOException {
        // init JS because JS needs to be initialized before it can be used
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context context = Context.newBuilder().out(out).build();
        context.initialize(JavaScriptLanguage.ID);


        BitwisePatternMeta meta = new BitwisePatternMeta(TruffleLanguageInformation.getLanguageInformation(JavaScriptLanguage.ID));
        // TODO #162 do a pattern detection for JS
    }
}
