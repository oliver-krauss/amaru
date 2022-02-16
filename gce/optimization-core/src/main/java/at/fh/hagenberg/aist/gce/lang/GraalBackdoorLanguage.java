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
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.ExecutableNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * @author Oliver Krauss on 25.12.2019
 */
@TruffleLanguage.Registration(id = GraalBackdoorLanguage.ID, name = "Graal Backdoor",
    version = "0.0", mimeType = {"application/backdoor"})
public class GraalBackdoorLanguage extends TruffleLanguage<GraalBackdoorContext> {

    public static final String ID = "GRAAL-BACKDOOR";

    public static final String CLASS = "BACKDOOR_CLASS";

    public static final String METHOD = "BACKDOOR_METHOD";

    public static final String ARGS = "BACKDOOR_ARGS";

    @Override
    protected GraalBackdoorContext createContext(Env env) {
        return new GraalBackdoorContext();
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        // we must override the threads classloader to prevent spring from using the wrong classloader
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        // We have hacked into Graal from here. Now it is time to do what the user actually wants
        Class clazz = this.getClass().getClassLoader().loadClass(System.getProperty(CLASS));
        String methodName = System.getProperty(METHOD);
        Method method = Arrays.stream(clazz.getDeclaredMethods()).filter(x -> methodName.equals(x.getName())).findFirst().orElseGet(null);
        String argsSerialized = System.getProperty(ARGS);
        Object[] args = null;

        if (argsSerialized != null) {
            args = (Object[]) SerializationUtil.deserialize(argsSerialized);
        }

        if (Modifier.isStatic(method.getModifiers())) {
            method.invoke(null, args);
        } else{
            System.out.println("Backdoor currently supports only static methods.");
        }

        // Finish up the program
        throw new GraalBackdoorException();
    }

}
