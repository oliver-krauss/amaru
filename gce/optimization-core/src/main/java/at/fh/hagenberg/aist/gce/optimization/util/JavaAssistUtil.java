/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Oliver Krauss on 02.01.2019
 */

public class JavaAssistUtil {

    /**
     * Helper function that returns value of field, while ensuring accessibility remains the same
     *
     * @param field to access in node
     * @param node  to access
     * @return value of node.field
     */
    public static Object safeFieldAccess(Field field, Object node) {
        if (field == null) {
            return null;
        }

        // make field accessible
        Boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object value = null;
        try {
            value = field.get(node);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // return field to original state
        field.setAccessible(accessible);

        return value;
    }

    /**
     * Helper function that returns value of field, while ensuring accessibility remains the same
     *
     * @param field to access in node
     * @param node  to access
     * @return value of node.field
     */
    public static void safeFieldWrite(Field field, Object node, Object writeValue) {
        if (field == null) {
            return;
        }

        // make field accessible
        Boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object value = null;
        try {
            field.set(node, writeValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // return field to original state
        field.setAccessible(accessible);
    }

    /**
     * Helper function that returns value of field by its name
     *
     * @param field name of field to be accessed
     * @param node  node to be accessed
     * @return value of node.field
     */
    public static void safeFieldWrite(String field, Object node, Object writeValue) {
        try {
            safeFieldWrite(node.getClass().getDeclaredField(field), node, writeValue);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function that returns value of field by its name
     *
     * @param field name of field to be accessed
     * @param node  node to be accessed
     * @return value of node.field
     */
    public static Object safeFieldAccess(String field, Object node) {
        try {
            return safeFieldAccess(node.getClass().getDeclaredField(field), node);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Attempts to find all fields containing the given string. Case INSENSITIVE.
     *
     * @param containedString part of fieldname to be found (case insensitive)
     * @param clazz           class to be searched
     * @return all fields matching (can be empty list)
     */
    public static List<Field> findFieldFuzzy(String containedString, Class clazz) {
        final String targetString = containedString.toLowerCase();
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(x -> x.getName().toLowerCase().contains(targetString)).collect(Collectors.toList());
        fields.addAll(Arrays.stream(clazz.getFields()).filter(x -> x.getName().toLowerCase().contains(targetString)).collect(Collectors.toList()));
        return fields;
    }

    /**
     * Attempts to find all fields a class has.
     *
     * @param clazz           class to be searched
     * @return all fields of class and superclasses
     */
    public static List<Field> getFields(Class clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    public static Class<?> safeFieldClassCheck(Field field) {
        try {
            return field.getType();
        } catch (Exception e) {
            System.out.println("Class not accessible. You can ignore this error");
            e.printStackTrace();
        }
        return null;
    }

    public static boolean safeAssignableCheck(Class clazz, Field field) {
        try {
            Class<?> fieldType = safeFieldClassCheck(field);
            return fieldType != null && clazz.isAssignableFrom(fieldType);
        } catch (Exception e) {
            System.out.println("Class not accessible. You can ignore this error");
            e.printStackTrace();
        }
        return false;
    }

    public static Field[] safeDeclaredFieldAccess(Class clazz) {
        try {
            return clazz.getDeclaredFields();
        } catch (Exception e) {
            System.out.println("Class not accessible. You can ignore this error");
            e.printStackTrace();
        }
        return new Field[0];
    }
}
