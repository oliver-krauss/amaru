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

import at.fh.hagenberg.aist.neo4j.reflective.converter.FieldConverter;
import science.aist.neo4j.annotations.Converter;
import org.neo4j.ogm.annotation.Id;

import java.lang.reflect.Field;

/**
 * Information class for the parameter of an {@link TruffleClassInitializer}
 *
 * @author Oliver Krauss on 11.01.2019
 */
public class TruffleParameterInformation {

    /**
     * Database ID
     */
    @Id
    protected Long id;

    /**
     * Exact type of the parameter for instantiation
     */
    private Class type;

    /**
     * Class that the parameter relates to.
     * May be "type" but if "type" is an array this is the component type of it.
     */
    private Class clazz;

    /**
     * Name of the paramter
     */
    private String name;

    /**
     * Field in class that actually maps to the parameter
     */
    @Converter(converter = FieldConverter.class)
    private Field field;

    /**
     * if the parameter is an array
     */
    private boolean array;

    /**
     * Constructor for DB do NOT USE OTHERWISE
     */
    public TruffleParameterInformation() {
    }

    public TruffleParameterInformation(Class type, String name, Field field) {
        this.type = type;
        this.name = name;
        this.field = field;
        this.clazz = type.isArray() ? type.getComponentType() : type;
        this.array = type.isArray();
    }


    public Class getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Class getClazz() {
        return clazz;
    }

    public Field getField() {
        return field;
    }

    public boolean isArray() {
        return array;
    }

    public Long getId() {
        return id;
    }
}
