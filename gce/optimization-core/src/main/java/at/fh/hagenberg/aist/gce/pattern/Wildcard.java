/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern;

public class Wildcard {

    /**
     * Wildcard representing embedded patterns. E.g. 1..n nodes
     */
    public static final String WILDCARD_ANYWHERE = "★";

    /**
     * Wildcard usable in place of the root node of a {@link com.oracle.truffle.api.TruffleLanguage}
     */
    public static final String WILDCARD_ANY_NODE = ".";

    /**
     * Negation of a type (e.g. not a write)
     */
    public static final String WILDCARD_NOT = "¬";
}
