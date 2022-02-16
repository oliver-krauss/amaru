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

import science.aist.seshat.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Oliver Krauss on 24.11.2019
 */

public class FieldConverter implements science.aist.neo4j.reflective.FieldConverter<String, Field> {

    @Override
    public void mapForDb(String name, Field value, Map map) {
        if (value != null) {
            map.put(name, value.getDeclaringClass().getName().replace('.', '|') + "|" + value.getName());
        }
    }

    @Override
    public Field toJavaValue(Field currentValue, Object newValue) {
        if (newValue == null) {
            return null;
        } else {
            try {
                String val = ((String) newValue).replace('|', '.');
                int idx = val.lastIndexOf(".");
                return Arrays.stream(Class.forName(val.substring(0, idx)).getDeclaredFields()).filter(x -> x.getName().equals(val.substring(idx + 1))).findFirst().orElse(null);
            } catch (ClassNotFoundException var4) {
                Logger.getInstance().error("Could not cast class from DB " + newValue, var4);
                return null;
            }
        }
    }
}
