/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.context;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Center for application context so we only have a single one in our execution
 *
 * @author Oliver Krauss on 07.05.2019
 */
public class ApplicationContextProvider {

    /**
     * Contexts that were already loaded
     */
    private static final Map<String, ClassPathXmlApplicationContext> contexts = new HashMap<>();

    /**
     * The default context for GCE, mostly contains the database connection
     * @return default context
     */
    public static ClassPathXmlApplicationContext getCtx() {
        return getCtx("truffleRepositoryConfig.xml");
    }

    /**
     * Returns the context of a specific xml file.
     * That context is UNIQUE and will always be returned as the same. If you included other config files
     * and return them separately they will be DIFFERENT contexts.
     * @param context to be loaded
     * @return context (singleton
     */
    public static ClassPathXmlApplicationContext getCtx(String context) {
        if (!contexts.containsKey(context)) {
            contexts.put(context, new ClassPathXmlApplicationContext(context));
        }
        return contexts.get(context);
    }

    /**
     * Shuts down all contexts to end the application safely
     */
    public static void close() {
        contexts.values().forEach(AbstractApplicationContext::close);
        contexts.clear();
    }

}
