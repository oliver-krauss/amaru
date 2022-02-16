/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.oracle.truffle.api;

/**
 * Helper class that allows access to the current language and context.
 *
 * @param <T> Truffle language
 * @param <C> Context of truffle language (the one in TruffleLanguage<C>)
 * @author Oliver Krauss on 27.12.2018
 */
public class TrufflePublicAccess<T, C> {

    private T language;

    private C context;

    private Class clazz;

    public TrufflePublicAccess(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * Helper function to access the protected getCurrentContext function, enabling us to get the language.
     *
     * @return
     */
    public T getLanguage() {
        if (language != null) {
            return language;
        }

        language = (T) TruffleLanguage.getCurrentLanguage(clazz);
        return language;
    }

    public C getCurrentContext() {
        if (context != null) {
            return context;
        }
        context = (C) TruffleLanguage.getCurrentContext(clazz);
        return context;
    }

}
