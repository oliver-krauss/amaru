/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.lang;

import at.fh.hagenberg.aist.gce.util.SerializationUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;

/**
 * Helper Class that utilizes the {@link GraalBackdoorLanguage} to execute stuff in Graal and still have access to Truffle etc.
 * Necessary as the Graal Class Loader is completely cast off from the rest of the infrastructure.
 * @author Oliver Krauss on 25.12.2019
 */
public class GraalBackdoorSystem {

    public boolean checkForGraal() {
        return "graal".equals(System.getProperty("jvmci.Compiler"));
    }

    public void breakIn(Class clazz, String function, Object[] params) {
        // prepare what backdoor should actually call
        System.setProperty(GraalBackdoorLanguage.CLASS, clazz.getName());
        System.setProperty(GraalBackdoorLanguage.METHOD, function);
        if (params != null) {
            System.setProperty(GraalBackdoorLanguage.ARGS, SerializationUtil.serialize(params));
        }

        // call the backdoor
        try {
            Context ctx = Context.newBuilder().build(); // .allowExperimentalOptions(true)
            ctx.initialize(GraalBackdoorLanguage.ID);
            ctx.eval(Source.create(GraalBackdoorLanguage.ID, "null"));
        } catch (PolyglotException e) {
            if (e.getMessage().equals("at.fh.hagenberg.aist.gce.lang.GraalBackdoorException")) {
                // that is what we want to happen -> SUCCESS
            } else {
                throw e;
            }
        }
    }
}
