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
 * @author Oliver Krauss on 06.01.2020
 */

public class JavassistInterceptProvider {

    private static JavassistInterceptCallback interceptor = null;

    public static JavassistInterceptCallback getInterceptor() {
        return interceptor;
    }

    public static JavassistInterceptCallback getInterceptor(Object requester) {
        return interceptor;
    }

    public static JavassistInterceptCallback getInterceptor(Object[] requester) {
        return interceptor;
    }

    public static void setInterceptor(JavassistInterceptCallback callback) {
        interceptor = callback;
    }

}
