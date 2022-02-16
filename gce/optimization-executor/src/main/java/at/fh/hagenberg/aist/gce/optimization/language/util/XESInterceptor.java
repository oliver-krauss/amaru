/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language.util;

import science.aist.seshat.Logger;
import com.oracle.truffle.api.nodes.Node;

/**
 * Default Interceptor for the JavassistWorker.
 * Calculates:
 * - the complexity measure of a test (the amount of nodes called, and the amount of nodes specialized)
 *
 * @author Oliver Krauss on 11.12.2019
 */
public class XESInterceptor extends JavassistInterceptor {

    private Logger logger;

    @Override
    public void beforeIntercept(Object target, String name, Object[] args) {
        super.beforeIntercept(target, name, args);
        String key = getNodePosition((Node) target);

        if (logger != null) {
            logger.error(key + " - " + target.getClass().getName() + "." + name, args);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
