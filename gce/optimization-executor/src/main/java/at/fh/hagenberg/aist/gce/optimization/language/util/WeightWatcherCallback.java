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

import com.oracle.truffle.api.CompilerDirectives;

/**
 * Callback with a truffle boundary, forcing graal to use our nodes.
 * @author Oliver Krauss on 05.01.2020
 */
public class WeightWatcherCallback {


    private static WeightWatcherCallback interceptor = new WeightWatcherCallback();

    public static WeightWatcherCallback getInterceptor() {
        return interceptor;
    }

    public static WeightWatcherCallback getInterceptor(Object caller) {
        return interceptor;
    }

    public static WeightWatcherCallback getInterceptor(Object[] caller) {
        return interceptor;
    }

    @CompilerDirectives.TruffleBoundary
    public void beforeIntercept() {
        // nuthin
    }
}
