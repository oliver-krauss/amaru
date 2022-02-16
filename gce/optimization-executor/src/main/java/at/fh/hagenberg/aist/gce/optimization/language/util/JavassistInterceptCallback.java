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

/**
 * Callback interface used to analyze nodes with the Javassist Execution
 * NOTE: Currently only supports an interception before a method is executed,
 * as post-interception would require knowing the entire method code
 *
 * @author Oliver Krauss on 11.12.2019
 */
public interface JavassistInterceptCallback {

    /**
     * will be called by any proxy BEFORE the method is executed
     *
     * @param target -> object which method was called
     * @param name   -> name of the method called
     * @param args   -> arguments given to the method (may be null)
     */
    public void beforeIntercept(Object target, String name, Object[] args);

}
