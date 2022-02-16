/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.parser;

/**
 * Type definition for the {@link com.oracle.truffle.api.dsl.NodeFactory} to help assigning types to classes.
 * Created by Oliver Krauss on 06.07.2016.
 */
public enum MinicBaseType {
    CHAR,
    INT,
    DOUBLE,
    FLOAT,
    STRING,
    VOID,
    ARRAY,
    STRUCT
}
