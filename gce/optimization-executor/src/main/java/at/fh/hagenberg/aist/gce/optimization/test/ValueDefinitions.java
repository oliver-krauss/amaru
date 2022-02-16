/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.test;

import at.fh.hagenberg.util.Pair;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Defines how we use our primitives system wide.
 * This is mostly required for handling in/output data which comes mostly as strings from files and interfaces
 *
 * @author Oliver Krauss on 31.10.2019
 */
public class ValueDefinitions {

    /**
     * Transform a given string into a java value and adds type information
     *
     * @param data to be transformed
     * @return Pair -> type, Java value
     */
    public static Pair<String, Object> stringToValueTyped(String data) {
        if (data.contains(":")) {
            String type = data.substring(0, data.indexOf(':'));
            String valueString = data.substring(data.indexOf(':') + 1).trim();
            Object value = null;

            switch (type) {
                case ("int"):
                    value = Integer.parseInt(valueString);
                    break;
                case ("char"):
                    value = valueString.charAt(0);
                    break;
                case ("float"):
                    value = Float.parseFloat(valueString);
                    break;
                case ("double"):
                    value = Double.parseDouble(valueString);
                    break;
                case "int_array":
                    String[] values = valueString.split(",");
                    int[] array = new int[values.length];
                    for (int i = 0; i < values.length; i++) {
                        array[i] = Integer.parseInt(values[i]);
                    }
                    value = array;
                    break;
                case "float_array":
                    String[] valuesF = valueString.split(",");
                    float[] arrayF = new float[valuesF.length];
                    for (int i = 0; i < valuesF.length; i++) {
                        arrayF[i] = Float.parseFloat(valuesF[i]);
                    }
                    value = arrayF;
                    break;
                default:
                    value = valueString; // do nothing
            }

            return new Pair<>(type, value);
        }
        if (data == null || data.equals("null")) {
            return new Pair<>("null", null);
        }
        throw new RuntimeException("Unknown type. Can not continue");
    }

    /**
     * Transform a given string into a java value
     *
     * @param data to be transformed
     * @return java value
     */
    public static Object stringToValue(String data) {
        return stringToValueTyped(data).getValue();
    }

    /**
     * Transforms a value into a string
     *
     * @param value to be transformed
     * @return value
     */
    public static String valueToString(Object value) {
        if (value == null) {
            return "null";
        }

        switch (value.getClass().getName()) {
            case ("java.lang.Integer"):
                return "int:" + value.toString();
            case ("java.lang.Character"):
                return "char:" + value.toString();
            case ("java.lang.Float"):
                return "float:" + value.toString();
            case ("java.lang.Double"):
                return "double:" + value.toString();
            case ("[I"):
                int[] intVal = ((int[])value);
                StringJoiner joiner = new StringJoiner(",");
                for (int i : intVal) {
                    joiner.add(String.valueOf(i));
                }
                return "int_array:" + joiner;
            case ("[F"):
                float[] floatVal = ((float[])value);
                StringJoiner floatJoiner = new StringJoiner(",");
                for (float f : floatVal) {
                    floatJoiner.add(String.valueOf(f));
                }
                return "float_array:" + floatJoiner;
            default:
                return "string:" + value.toString();
        }
    }


}
