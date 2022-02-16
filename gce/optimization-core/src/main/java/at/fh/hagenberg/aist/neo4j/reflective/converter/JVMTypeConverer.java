/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.neo4j.reflective.converter;

import at.fh.hagenberg.aist.gce.optimization.runtime.JVMType;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import science.aist.neo4j.reflective.FieldConverter;

import java.util.Map;

/**
 * @author Oliver Krauss on 24.11.2019
 */

public class JVMTypeConverer implements FieldConverter<String, JVMType> {

    @Override
    public void mapForDb(String name, JVMType value, Map<String, Object> map) {
        if (value != null) {
            map.put(name, value.name());
        }
    }

    @Override
    public JVMType toJavaValue(JVMType currentValue, Object newValue) {
        if (newValue != null) {
            return JVMType.valueOf((String)newValue);
        }
        return null;
    }
}
