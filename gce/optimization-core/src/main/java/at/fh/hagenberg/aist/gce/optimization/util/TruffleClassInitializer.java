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

import science.aist.neo4j.annotations.Converter;
import at.fh.hagenberg.aist.neo4j.reflective.converter.ConstructorConverter;
import at.fh.hagenberg.aist.neo4j.reflective.converter.MethodConverter;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.nodes.Node;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;


/**
 * Support class that contains ONE valid initialization method for a {@link TruffleClassInformation}
 *
 * @author Oliver Krauss on 11.01.2019
 */
@NodeEntity
public class TruffleClassInitializer {

    /**
     * Database ID
     */
    @Id
    protected Long id;

    /**
     * if the initializer is a method or not
     */
    protected boolean isMethod;

    /**
     * Create method handled by this initializer
     */
    @Converter(converter = MethodConverter.class)
    protected Method createMethod;

    /**
     * Constructor handled by this initializer
     */
    @Converter(converter = ConstructorConverter.class)
    protected Constructor createConstructor;

    /**
     * Information this class is part of
     */
    @Relationship(type = "CLASS")
    protected TruffleClassInformation clazz;

    @Relationship(type = "PARAMETER")
    protected TruffleParameterInformation[] parameters;

    /**
     * the minimal size a subtree created from this constructor can be.
     * Defaults to 1 (no child nodes)
     */
    protected int minimalSubtreeSize = -1;

    /**
     * Initialization Method for Db. DO NOT USE OTHERWISE
     */
    public TruffleClassInitializer() {
    }

    public TruffleClassInitializer(TruffleClassInformation clazz, Method method) {
        isMethod = true;
        this.clazz = clazz;
        this.createMethod = method;
        identifyParameters();
    }

    public TruffleClassInitializer(TruffleClassInformation clazz, Constructor constructor) {
        isMethod = false;
        this.clazz = clazz;
        this.createConstructor = constructor;
        identifyParameters();
    }

    /**
     * Helper function that collects the parameter information of this initializer
     */
    private void identifyParameters() {
        Parameter[] params = isMethod ? createMethod.getParameters() : createConstructor.getParameters();
        parameters = new TruffleParameterInformation[params.length];
        String[] parameterNames = new String[params.length];

        try {
            // get original classname, as javassist can't look at its own classes!
            String creatorClass = isMethod ? createMethod.getDeclaringClass().getName() : createConstructor.getDeclaringClass().getName();
            String className =
                (isMethod && NodeFactory.class.isAssignableFrom(this.createMethod.getDeclaringClass())) ?  // check if we are dealing with a node factory
                    this.createMethod.getDeclaringClass().getName() :  // load from node factory
                    creatorClass; // load from class (don't use clazz, as there can be constructors in superclasses

            // Lookup the method info of the create method, or alternatively the constructor
            MethodInfo methodInfo = isMethod ? Arrays.stream(ClassPool.getDefault().getCtClass(className).getMethods()).filter(x -> x.getName().equals(createMethod.getName()) && compareParameters(x)).findFirst().get().getMethodInfo() :
                Arrays.stream(ClassPool.getDefault().getCtClass(className).getConstructors()).filter(x -> compareParameters(x)).findFirst().get().getMethodInfo();

            // get the attribute table (see https://stackoverflow.com/questions/20316965/get-a-name-of-a-method-parameter-using-javassist)
            LocalVariableAttribute attribute = (LocalVariableAttribute) methodInfo.getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
            int length = attribute != null ? attribute.tableLength() : 0;

            // get every name. Note that create methods don't have a "this" but constructors do, also some create methods have odd indices before the paramters (only observed in generics)
            int j = 0;
            for (int i = length - parameterNames.length; i < length; i++) {
                parameterNames[j] = (methodInfo.getConstPool().getUtf8Info(attribute.nameIndex(i)));
                j++;
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
        }

        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = new TruffleParameterInformation(params[i].getType(), parameterNames[i], findAssociatedField(params[i].getType(), parameterNames[i]));
        }
    }

    private boolean compareParameters(CtBehavior x) {
        try {
            CtClass[] checkTypes = x.getParameterTypes();
            Class<?>[] actualTypes = isMethod ? createMethod.getParameterTypes() : createConstructor.getParameterTypes();
            if (actualTypes.length == checkTypes.length) {
                for (int i = 0; i < actualTypes.length; i++) {
                    if (checkTypes[i].isArray() != actualTypes[i].isArray()) {
                        return false;
                    }
                    String checkClass = checkTypes[i].isArray() ? checkTypes[i].getComponentType().getName() : checkTypes[i].getName();
                    String actualClass = actualTypes[i].isArray() ? actualTypes[i].getComponentType().getName() : actualTypes[i].getName();
                    if (!checkClass.equals(actualClass)) {
                        return false;
                    }
                }
                return true;
            }
        } catch (NotFoundException e) {
            Logger.log(Logger.LogLevel.ERROR, "CtClass not found. This should never ever happen");
        }
        return false;
    }

    private Field findAssociatedField(Class type, String parameterName) {
        // TODO #87 the paramterization has some problems as the parameter can actually be "Object" and the constructor checks the specific type of class
        // TODO #87 Switch over from field name mapping to creating nodes and tracing the parameters to fields

        // make name lowercase for more generous matching
        String finalParameterName = parameterName.toLowerCase();
        Class fieldClass = this.clazz.getClazz();

        // search through the class and all it's parent classes for a field that matches the parameter name
        try {
            while (fieldClass != null) {
                // friendly match argument and field can be the same, or be a substring of each other
                Optional<Field> matchField = Arrays.asList(fieldClass.getDeclaredFields()).stream().filter(x ->
                        x.getType().equals(type) // match for type
                                && (finalParameterName.contains(x.getName().toLowerCase()) || x.getName().toLowerCase().contains(finalParameterName)) // match for name
                ).findAny();
                if (matchField.isPresent()) {
                    if (
                            !matchField.get().getType().getName().startsWith("com.oracle.truffle.api.profiles") && // we want no truffle specific profiles
                                    !matchField.get().isSynthetic()) { // we want no synthetic (added by compiler, or framework etc.) fields.
                        return matchField.get();
                    }
                }
                fieldClass = fieldClass.getSuperclass();
            }
        } catch (Throwable e) {
            System.out.println("ERROR THE LOADING FAILED");
        }

        return null;
    }

    /**
     * Creates an Object of the class this initializer is associated with
     *
     * @param parameters for creation
     * @return object or null if creation was not possible
     */
    public Object instantiate(Object[] parameters) {
        try {
            return isMethod ? createMethod.invoke(null, parameters) : createConstructor.newInstance(parameters);
        } catch (Exception e) {
            System.out.println("Instantiation of " + this.getClazz() + " failed.");
            System.out.println("param " + (parameters == null ? "null" : parameters.length));
            if (parameters != null){
                for (Object parameter : parameters) {
                    System.out.println(parameter == null ? "null" : parameter.getClass().getName() + " " + parameter);
                }
            }
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the minimal size a subtree with this class as head can can have.
     * If the size can be determined statically it is alway >= 1 as it counts itself.
     *
     * @return minimal size or -1 if the size can't be determined
     */
    public int getMinimalSubtreeSize() {
        if (minimalSubtreeSize > 0) {
            return minimalSubtreeSize;
        }

        // find minimal subtree size.
        int minSize = 0;
        for (TruffleParameterInformation parameter : parameters) {
            // only check paramters of node class
            // We care about arrays as arrays can be empty, but most nodes fail with an empty array.
            if (Node.class.isAssignableFrom(parameter.getClazz())) {
                // find valid nodes from TLI and select smallest possible outcome
                Class requiredClass = parameter.getClazz();
                List<Class> classes = clazz.getContext().getOperators().get(requiredClass);
                if (classes == null) {
                    // we can't determine the depth if we have no operators
                    return -1;
                }
                int minSizeForParam = classes.stream().mapToInt(x -> TruffleClassInformation.informationForClass(clazz.getContext(), x).getMinimalSubtreeSizeNoPropagation()).min().orElse(-1);
                if (minSizeForParam > minSize) {
                    // the largest parameter is the minimal size of this class
                    minSize = minSizeForParam;
                }
                if (minSizeForParam < 0) {
                    // if any of the parameters is < 0 we can't determine the minimal size; Except if any other is 1 (which is the absolutely possible minimum anyway).
                    OptionalInt min = classes.stream().mapToInt(x -> TruffleClassInformation.informationForClass(clazz.getContext(), x).getMinimalSubtreeSizeNoPropagation()).filter(x -> x > 0).min();
                    if (!min.isPresent() || min.getAsInt() != 1) {
                        return -1;
                    } else {
                        minSize = 1;
                    }
                }
            }
        }
        return this.minimalSubtreeSize = minSize + 1; // +1 to count itself
    }

    public TruffleParameterInformation[] getParameters() {
        if (parameters == null) {
            return new TruffleParameterInformation[0];
        }
        return parameters;
    }

    public Class getClazz() {
        return clazz.getClazz();
    }

    public TruffleClassInformation getClassInfo() {
        return clazz;
    }

    public boolean isMethod() {
        return isMethod;
    }

    public String getName() {
        if (isMethod) {
            return createMethod.getName();
        } else {
            return "constructor";
        }
    }

    public Long getId() {
        return id;
    }
}
