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

import science.aist.neo4j.reflective.FieldConverter;
import science.aist.seshat.Logger;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Oliver Krauss on 24.11.2019
 */

public class ConstructorConverter implements FieldConverter<String, Constructor> {

    @Override
    public void mapForDb(String name, Constructor value, Map map) {
        if (value != null) {
            Constructor[] declaredConstructors = value.getDeclaringClass().getDeclaredConstructors();
            int i;
            for (i = 0; i < declaredConstructors.length; i++) {
                if (value.equals(declaredConstructors[i])) {
                    break;
                }
            }
            map.put(name, value.getDeclaringClass().getName().replace('.', '|') + "|" + i);
        }
    }

    @Override
    public Constructor toJavaValue(Constructor currentValue, Object newValue) {
        if (newValue == null) {
            return null;
        } else {
            try {
                String val = ((String) newValue).replace('|', '.');
                int idx = val.lastIndexOf(".");
                return Class.forName(val.substring(0, idx)).getDeclaredConstructors()[Integer.valueOf(val.substring(idx +1))];
            } catch (ClassNotFoundException var4) {
                Logger.getInstance().error("Could not cast class from DB " + newValue, var4);
                return null;
            }
        }
    }
}
