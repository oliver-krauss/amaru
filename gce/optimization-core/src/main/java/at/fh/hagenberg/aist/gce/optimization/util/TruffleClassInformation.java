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


import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import science.aist.neo4j.annotations.Converter;
import at.fh.hagenberg.aist.neo4j.reflective.converter.TruffleClassPropertyConverter;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.profiles.ConditionProfile;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper Class that initializes a Truffle Node
 */
@NodeEntity
public class TruffleClassInformation {

    /**
     * Database ID
     */
    @Id
    protected Long id;

    /**
     * Class represented here
     */
    private Class clazz;

    /**
     * Context this class information was created in.
     */
    @Transient // because the provider is irrelevant for the db
    private TruffleLanguageContextProvider context;

    /**
     * All valid initializers for this class
     */
    @Relationship(type = "INITIALIZER")
    private List<TruffleClassInitializer> initializers = new ArrayList<>();

    /**
     * the minimal size a subtree created from this class can be.
     * Defaults to 1 (no child nodes)
     */
    private int minimalSubtreeSize = -1;

    /**
     * Short name of class from NodeInformation
     */
    private String shortName;

    /**
     * Description of class from NodeInformation
     */
    private String description;

    /**
     * The weight (run-time cost) of the node
     * Map<Where the weighting happened, weight measured on system>
     */
    @Relationship(type = "WEIGHT")
    protected Map<SystemInformation, Double> weight = new HashMap<>();

    /**
     * The unoptimized weight of the node (run-time cost before graal optimization)
     * Map<Where the weighting happened, weight measured on system>
     */
    @Relationship(type = "WEIGHT_UNOPTIMIZED")
    protected Map<SystemInformation, Double> weightUnoptimized = new HashMap<>();

    /**
     * Properties of the class such as statefulness or leaving the program scope
     */
    @Converter(converter = TruffleClassPropertyConverter.class, overrides = TruffleClassProperty.class)
    private List<TruffleClassProperty> properties = new ArrayList<>();

    /**
     * If the class is of type {@link TruffleClassProperty#STATE_READ} these pairings are acceptable
     * to produce a write in the same frameslot.
     */
    @Relationship(type = "WRITE_PAIRINGS")
    private List<TruffleClassInformation> writePairings = new ArrayList<>();

    /**
     * If the class is of type {@link TruffleClassProperty#STATE_READ_ARGUMENT} this list contains
     * the classes of all arguments this class can read
     */
    private List<String> argumentReadClasses = new ArrayList<>();

    /**
     * Constructor for Database DO NOT CALL OTHERWISE.
     * WARNING DOES NOT CALL INITIALIZERS
     */
    public TruffleClassInformation() {
    }

    protected TruffleClassInformation(Class clazz, TruffleLanguageContextProvider context) {
        if (!classInfo.containsKey(context)) {
            classInfo.put(context, new HashMap<>());
        }
        classInfo.get(context).put(clazz, this);

        // call expensive initializers
        callTargetClasses = initCallTargetClasses();


        this.clazz = clazz;
        this.context = context;

        // Load class initializers
        List<Method> methods = findPublicStatic(clazz.getDeclaredMethods());
        methods.addAll(findPublicStatic(clazz.getMethods()));
        methods.stream().filter(x -> {
            boolean acceptable = false;
            Class hierarchy = clazz;
            while (!acceptable && hierarchy != Node.class && hierarchy != null) {
                acceptable = hierarchy.equals(x.getReturnType());
                hierarchy = hierarchy.getSuperclass();
            }
            return acceptable;
        }).distinct().forEach(x -> initializers.add(new TruffleClassInitializer(this, x)));

        // load constructor initializers
        findPublic(clazz.getConstructors()).forEach(x -> initializers.add(new TruffleClassInitializer(this, x)));

        // load node factory initializers
        if (clazz.getEnclosingClass() != null && Arrays.asList(clazz.getEnclosingClass().getInterfaces()).contains(NodeFactory.class)) {
            findPublicStatic(clazz.getEnclosingClass().getDeclaredMethods()).stream().filter(x -> {
                boolean acceptable = false;
                Class hierarchy = clazz;
                while (!acceptable && hierarchy != Node.class && hierarchy != null) {
                    acceptable = hierarchy.equals(x.getReturnType());
                    hierarchy = hierarchy.getSuperclass();
                }
                return acceptable;
            }).distinct().forEach(x -> initializers.add(new TruffleClassInitializer(this, x)));
        }
    }

    /**
     * Control flow exception classes, loaded only
     */
    @Transient
    private static List<String> controlFlowExceptionClasses = findControlFlowExceptionClasses();

    private static List<String> findControlFlowExceptionClasses() {
        ClassLoadingHelper classLoadingHelper = new ClassLoadingHelper(Arrays.asList("com.oracle.truffle", "at.fh.hagenberg"));
        classLoadingHelper.setExcludes(Arrays.asList("com.oracle.truffle.polyglot", "com.oracle.truffle.api", "com.oracle.truffle.tck", "gce.optimization", "gce.science", "gce.benchmark", "gce.lang.optimization"));
        classLoadingHelper.setParentClasses(Collections.singletonList(ControlFlowException.class));
        controlFlowExceptionClasses = new ArrayList<>();
        classLoadingHelper.findClasses().forEach(x -> controlFlowExceptionClasses.add(x.getName()));
        return controlFlowExceptionClasses;
    }

    @Transient
    private static List<String> readOperations;

    @Transient
    private static List<String> writeOperations;

    /**
     * Frame Classes loaded only once
     */
    @Transient
    private static List<String> frameClasses = findFrameClasses();

    private static List<String> findFrameClasses() {
        readOperations = Arrays.asList("getArguments", "getObject", "getByte", "getBoolean", "getInt", "getLong", "getFloat", "getDouble",
                "getObjectSafe", "getByteSafe", "getBooleanSafe", "getIntSafe", "getLongSafe", "getFloatSafe", "getDoubleSafe");
        writeOperations = Arrays.asList("setObject", "setByte", "setBoolean", "setInt", "setLong", "setFloat", "setDouble");

        ClassLoadingHelper classLoadingHelper = new ClassLoadingHelper(Arrays.asList("com.oracle.truffle", "at.fh.hagenberg"));
        classLoadingHelper.setExcludes(Arrays.asList("com.oracle.truffle.polyglot", "com.oracle.truffle.tck", "gce.optimization", "gce.science", "gce.benchmark", "gce.lang.optimization"));
        classLoadingHelper.setParentClasses(Collections.singletonList(com.oracle.truffle.api.frame.Frame.class));
        frameClasses = new ArrayList<>();
        classLoadingHelper.findClasses().forEach(x -> {
            frameClasses.add(x.getName());
        });
        // also add frame util which is often used instead of regular read
        frameClasses.add(FrameUtil.class.getName());

        List<String> qualifiedReadOperations = new ArrayList<>();
        List<String> qualifiedWriteOperations = new ArrayList<>();

        // attempt to find additional frame utils
        classLoadingHelper.setParentClasses(Collections.singletonList(Object.class));
        List<Class> potentialHelperClasses = classLoadingHelper.findClasses();
        potentialHelperClasses.forEach(x -> {
            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass ctClass = cp.get(x.getName());
                CtMethod[] methods = ctClass.getDeclaredMethods();
                for (CtMethod method : methods) {
                    // we assume helper methods are static ONLY
                    if (Modifier.isStatic(method.getModifiers())) {
                        method.instrument(
                                new ExprEditor() {
                                    public void edit(MethodCall m)
                                            throws CannotCompileException {
                                        // find reads and writes. Note that a class can do BOTH
                                        if (frameClasses.contains(m.getClassName())) {
                                            if (readOperations.contains(m.getMethodName())) {
                                                // This is a read helper
                                                qualifiedReadOperations.add(ctClass.getName() + "." + method.getName());
                                            }
                                            if (writeOperations.contains(m.getMethodName())) {
                                                qualifiedWriteOperations.add(ctClass.getName() + "." + method.getName());
                                            }
                                        }
                                    }
                                });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // qualify operations
        frameClasses.forEach(clazz -> {
            readOperations.forEach(op -> {
                qualifiedReadOperations.add(clazz + "." + op);
            });
            writeOperations.forEach(op -> {
                qualifiedWriteOperations.add(clazz + "." + op);
            });
        });

        readOperations = qualifiedReadOperations;
        writeOperations = qualifiedWriteOperations;

        return frameClasses;
    }

    private List<Class> callTargetClasses = initCallTargetClasses();

    private List<Class> initCallTargetClasses() {
        List<Class> ctClasses = new ArrayList<>();

        ClassLoadingHelper classLoadingHelper = new ClassLoadingHelper(Arrays.asList("com.oracle.truffle", "at.fh.hagenberg"));
        classLoadingHelper.setExcludes(Arrays.asList("com.oracle.truffle.polyglot", "com.oracle.truffle.api", "com.oracle.truffle.tck", "gce.optimization", "gce.science", "gce.benchmark", "gce.lang.optimization"));
        classLoadingHelper.setParentClasses(Collections.singletonList(TruffleObject.class));

        classLoadingHelper.findClasses().forEach(x -> {
            Class parent = x;
            boolean found = false;
            while (parent != Object.class && !found) {
                found = Arrays.stream(parent.getDeclaredFields()).anyMatch(field -> JavaAssistUtil.safeFieldClassCheck(field) != null && JavaAssistUtil.safeAssignableCheck(CallTarget.class, field));
                parent = parent.getSuperclass();
            }
            if (found) {
                ctClasses.add(x);
            }
        });

        return ctClasses;
    }

    protected void loadMetadata() {

        Class parent = clazz;
        boolean boundary = false;
        final boolean[] controlFlow = {false};
        final boolean[] controlFlowException = {false};
        final boolean[] read = {false};
        final boolean[] readArgs = {false};
        final boolean[] write = {false};
        boolean global = false;
        boolean branch = false;
        boolean functionCall = false;

        // check for all the stuff we need to look for in classes and superclasses
        while (parent != Node.class && parent != RootNode.class) {
            if (!boundary) {
                // determine if we run into a truffle boundary
                boundary = Arrays.stream(parent.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(CompilerDirectives.TruffleBoundary.class));
                if (boundary) {
                    properties.add(TruffleClassProperty.TRUFFLE_BOUNDARY);
                }
            }
            if (!branch) {
                // check if we can find a branching statement
                branch = Arrays.stream(JavaAssistUtil.safeDeclaredFieldAccess(parent)).anyMatch(field -> JavaAssistUtil.safeAssignableCheck(ConditionProfile.class, field));
                if (branch) {
                    properties.add(TruffleClassProperty.BRANCH);
                    controlFlow[0] = true;
                }
            }
            if (!functionCall) {
                functionCall = Arrays.stream(JavaAssistUtil.safeDeclaredFieldAccess(parent)).anyMatch(field -> callTargetClasses.stream().anyMatch(x -> JavaAssistUtil.safeAssignableCheck(x, field)));
                if (functionCall) {
                    properties.add(TruffleClassProperty.FUNCTION_CALL);
                    controlFlow[0] = true;
                }
            }
            if (!global) {
                // we assume that ALL classes containing a MaterializedFrame read/write from the global state
                global = Arrays.stream(JavaAssistUtil.safeDeclaredFieldAccess(parent)).anyMatch(field -> JavaAssistUtil.safeAssignableCheck(MaterializedFrame.class, field));
            }
            if (!controlFlowException[0] || !read[0] || !write[0]) {
                try {
                    ClassPool cp = ClassPool.getDefault();
                    CtClass ctClass = cp.get(parent.getName());
                    CtMethod[] methods = ctClass.getDeclaredMethods();
                    for (CtMethod method : methods) {
                        method.instrument(
                                new ExprEditor() {
                                    public void edit(MethodCall m)
                                            throws CannotCompileException {
                                        // find reads and writes. Note that a class can do BOTH
                                        if (readOperations.contains(m.getClassName() + "." + m.getMethodName())) {
                                            read[0] = true;
                                            // we need to cheat for arrays as they are READ and the written to withouth frame.write
                                            if (method.getName().contains("write")) {
                                                // assume this is also a write operation
                                                write[0] = true;
                                            }
                                            // we need to detect argument reads differntly from regular reads
                                            if (m.getMethodName().equals("getArguments")) {
                                                readArgs[0] = true;
                                            }
                                        }
                                        if (writeOperations.contains(m.getClassName() + "." + m.getMethodName())) {
                                            write[0] = true;
                                        }
                                    }

                                    public void edit(NewExpr e)
                                            throws CannotCompileException {
                                        // find control flow exceptions
                                        if (controlFlowExceptionClasses.contains(e.getClassName())) {
                                            controlFlowException[0] = true;
                                            properties.add(TruffleClassProperty.CONTROL_FLOW_EXCEPTION);
                                            controlFlow[0] = true;
                                        }
                                    }
                                });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            parent = parent.getSuperclass();
        }

        // consolidate read/write info
        if (read[0] || write[0]) {
            if (global) {
                properties.add(TruffleClassProperty.GLOBAL_STATE);
            } else {
                properties.add(TruffleClassProperty.LOCAL_STATE);
            }
            if (read[0]) {
                properties.add(TruffleClassProperty.STATE_READ);
            }
            if (readArgs[0]) {
                properties.add(TruffleClassProperty.STATE_READ_ARGUMENT);
            }
            if (write[0]) {
                properties.add(TruffleClassProperty.STATE_WRITE);
            }
        }

        // Find if a loop node is present
        if (RepeatingNode.class.isAssignableFrom(clazz)) {
            properties.add(TruffleClassProperty.LOOP);
            controlFlow[0] = true;
        }
        if (controlFlow[0]) {
            // assign the control flow proeprty
            properties.add(TruffleClassProperty.CONTROL_FLOW);
        }

        // Find root-class if we are dealing with a generated one
        GeneratedBy gb = (GeneratedBy) clazz.getDeclaredAnnotation(GeneratedBy.class);
        Class clazzForNodeInfo = clazz;
        if (gb != null) {
            clazzForNodeInfo = gb.value();
        }

        // find node info and read metadata
        NodeInfo info = (NodeInfo) clazzForNodeInfo.getDeclaredAnnotation(NodeInfo.class);
        if (info != null) {
            this.shortName = info.shortName();
            this.description = info.description();
        }
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
        return minimalSubtreeSize = initializers.stream().mapToInt(TruffleClassInitializer::getMinimalSubtreeSize).filter(x -> x > 0).min().orElse(-1);
    }

    /**
     * Returns the minimal size a subtree with this class as head can can have.
     * it only returns the size if it is known, and does not calculate it.
     *
     * @return minimal size or -1 if the size can't be determined
     */
    protected int getMinimalSubtreeSizeNoPropagation() {
        return minimalSubtreeSize;
    }

    /**
     * Class info that was already loaded will not be rebuilt
     * Class info IS context-sensitive
     */
    @Transient // is just a cache map
    private static Map<TruffleLanguageContextProvider, Map<Class, TruffleClassInformation>> classInfo = new HashMap<>();

    public static TruffleClassInformation informationForClass(TruffleLanguageContextProvider context, String clazz) {
        return informationForClass(context, ClassLoadingHelper.loadClassByName(clazz));
    }

    public static TruffleClassInformation informationForClass(TruffleLanguageContextProvider context, Class clazz) {
        if (context == null) {
            throw new RuntimeException("Generating a Class information without context is not possible");
        }

        if (classInfo.containsKey(context)) {
            Map<Class, TruffleClassInformation> contextSensitiveInformation = classInfo.get(context);
            if (contextSensitiveInformation.containsKey(clazz)) {
                return contextSensitiveInformation.get(clazz);
            }
        }
        return new TruffleClassInformation(clazz, context);
    }

    /**
     * Finds all public executables in given array
     *
     * @param executable Constructor or Modifier
     * @param <T>        Executable type
     * @return any public executable
     */
    private static <T extends Executable> List<T> findPublic(T[] executable) {
        return Arrays.stream(executable).filter(x -> Modifier.isPublic(x.getModifiers())).collect(Collectors.toList());
    }

    /**
     * Finds all public static executables of class
     *
     * @param executable Constructor or Modifier
     * @param <T>        Executable type
     * @return any public executable, or the first private one
     */
    private static <T extends Executable> List<T> findPublicStatic(T[] executable) {
        return Arrays.stream(executable).filter(x -> Modifier.isPublic(x.getModifiers()) && Modifier.isStatic(x.getModifiers())).collect(Collectors.toList());
    }


    public Class getClazz() {
        return clazz;
    }

    public boolean isInstantiable() {
        return initializers.size() > 0;
    }

    public List<TruffleClassInitializer> getInitializers() {
        return initializers;
    }

    @Transient // just for caching
    private List<TruffleClassInitializer> proxyInitializers;

    /**
     * Selects the initializers best suited for proxying a given node
     * This is the initializers with the all associated fields known sorted by the amount of parameters (most to least)
     *
     * @return initializers best suited for proxying
     */
    public List<TruffleClassInitializer> getInitializersForProxying() {
        if (proxyInitializers != null) {
            return proxyInitializers;
        }
        proxyInitializers = initializers.stream().filter(x -> Arrays.stream(x.getParameters()).allMatch(y -> y.getField() != null)).collect(Collectors.toList());
        if (proxyInitializers.size() == 0) {
            // as fallback use any initializer available
            proxyInitializers = initializers;
        }
        return proxyInitializers;
    }

    @Transient // just for caching
    private List<TruffleClassInitializer> creationInitializers;

    /**
     * Selects the initializers best suited for creating new nodes
     * This is the initializers sorted by the least parameters with all parameters representing Node Classes or Known Terminals
     *
     * @return initializer best suited for creation
     */
    public List<TruffleClassInitializer> getInitializersForCreation() {
        if (creationInitializers != null) {
            return creationInitializers;
        }
        creationInitializers = new ArrayList<>(initializers.stream().filter(x -> Arrays.stream(x.getParameters()).allMatch(param ->
                // TODO #74 -> fix magic strings.
                Node.class.isAssignableFrom(param.getClazz()) || param.getClazz().getName().equals("int") ||
                        param.getClazz().getName().equals("char") || param.getClazz().getName().equals("double") ||
                        param.getClazz().getName().equals("float") || param.getClazz().getName().equals("string") ||
                        param.getClazz().getName().equals("java.lang.String")
        )).collect(Collectors.toList()));

        if (creationInitializers.size() == 0) {
            // as fallback use any initializer available
            creationInitializers = initializers;
        }

        creationInitializers.sort(Comparator.comparingInt(o -> o.getParameters().length));
        return creationInitializers;
    }

    public TruffleLanguageContextProvider getContext() {
        return context;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public List<TruffleClassProperty> getProperties() {
        return properties;
    }

    public Map<SystemInformation, Double> getWeight() {
        return weight;
    }

    public Map<SystemInformation, Double> getWeightUnoptimized() {
        return weightUnoptimized;
    }

    public double getSystemWeight() {
        return this.weight.getOrDefault(SystemInformation.getCurrentSystem(), -1.0);
    }

    public double getSystemWeightUnoptimized() {
        return this.weightUnoptimized.getOrDefault(SystemInformation.getCurrentSystem(), -1.0);
    }

    protected void setSystemWeight(double weight) {
        this.weight.put(SystemInformation.getCurrentSystem(), weight);
    }

    protected void setSystemWeightUnoptimized(double weightUnoptimized) {
        this.weightUnoptimized.put(SystemInformation.getCurrentSystem(), weightUnoptimized);
    }

    public boolean hasProperty(TruffleClassProperty property) {
        return properties != null && properties.contains(property);
    }

    // Transient because this is just a helper for Write Pairings and we don't support bi-directional relationships yet
    @Transient
    private List<TruffleClassInformation> readPairings = null;

    /**
     * Finds all reads that have a write pairing to this class
     *
     * @return
     */
    public List<TruffleClassInformation> getReadPairings() {
        if (readPairings == null) {
            readPairings = new ArrayList<>();
            this.getContext().getInstantiableNodes().values().parallelStream().forEach(x -> {
                if (x.getWritePairings().contains(this)) {
                    readPairings.add(x);
                }
            });
        }
        return readPairings;
    }

    public List<TruffleClassInformation> getWritePairings() {
        return writePairings;
    }

    public List<String> getArgumentReadClasses() {
        return argumentReadClasses;
    }

    @Override
    public String toString() {
        return "TruffleClassInformation{" +
                "id=" + id +
                ", clazz=" + clazz +
                '}';
    }

    /**
     * Copies the entire class information but does not initialize values dependent on other TCIs such as the minimal subtree size
     *
     * @return copy of class information
     */
    public TruffleClassInformation copy(TruffleLanguageContextProvider context) {
        TruffleClassInformation truffleClassInformation = new TruffleClassInformation();
        truffleClassInformation.clazz = this.clazz;
        truffleClassInformation.context = context;
        truffleClassInformation.initializers = new ArrayList<>(initializers);
        truffleClassInformation.shortName = shortName;
        truffleClassInformation.description = description;
        truffleClassInformation.weight = weight;
        truffleClassInformation.weightUnoptimized = weightUnoptimized;
        truffleClassInformation.properties = new ArrayList<>(properties);
        truffleClassInformation.writePairings = new ArrayList<>(writePairings);
        truffleClassInformation.argumentReadClasses = new ArrayList<>(argumentReadClasses);
        truffleClassInformation.callTargetClasses = new ArrayList<>(callTargetClasses);
        truffleClassInformation.proxyInitializers = proxyInitializers != null ? new ArrayList<>(proxyInitializers) : null;
        truffleClassInformation.creationInitializers = creationInitializers != null ? new ArrayList<>(creationInitializers) : null;
        return truffleClassInformation;
    }


}
