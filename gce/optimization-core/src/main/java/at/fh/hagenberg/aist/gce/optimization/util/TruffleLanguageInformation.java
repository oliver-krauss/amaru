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
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.nodes.Node;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains information on the Nodes of a Truffle Language
 * Created by Oliver Krauss on 23.02.2017.
 */
@NodeEntity
public class TruffleLanguageInformation implements TruffleLanguageContextProvider {

    /**
     * Database ID
     */
    @Id
    protected Long id;

    // region fields

    /**
     * List of all node classes that are contained in this information object
     */
    @Relationship(type = "ALL_CLASSES")
    protected Map<Class, TruffleClassInformation> nodeClasses = new HashMap<>();

    /**
     * List of all node classes that are contained in this information object AND are actually instantiable
     */
    @Relationship(type = "INSTANTIABLE_CLASSES")
    protected Map<Class, TruffleClassInformation> instantiableNodeClasses = new HashMap<>();

    /**
     * Map of all classes that are currently unreachable. The map contains the class and<y the reason it is unreachable
     * Is the Diff between Node-Classes and InstantiableNodeclasses, as well as classes not currently reachable by the strategies
     * the collection is mainly meant for developers, to see where work still needs to be done
     */
    protected Map<Class, String> unreachableClasses = new HashMap<>();

    /**
     * List of all node classes that only contain terminal values
     */
    protected List<Class> terminalNodeClasses = new ArrayList<>();

    /**
     * This map contains a list of parent classes for which there are specific instantiations.
     * Key = (Abstract) Class being searched for
     * Value = List of classes that extend this class
     */
    protected Map<Class, List<Class>> instantiableClasses = new HashMap<>();

    /**
     * This map contains a list of terminal classes for which there are specific instantiations.
     * Key = (Abstract) Class being searched for
     * Value = List of classes that extend this class
     */
    protected Map<Class, List<Class>> instantiableTerminalClasses = new HashMap<>();

    /**
     * Map of node classes containing only the sub-classes of the given class (and interfaces)
     * Key = class or interface; Value = all subclasses thereof
     */
    protected Map<Class, List<Class>> classHierarchy = new HashMap<>();

    /**
     * Map of node classes containing only the sub-classes of the given class (and interfaces)
     * Key = class or interface; Value = all subclasses thereof
     *
     * Unlike classHierarchy this is created via getEnclosingClass() preferred over getSuperclass()
     */
    protected Map<Class, List<Class>> enclosingHierarchy = new HashMap<>();

    /**
     * Truffle ID of the Truffle language this information is about
     */
    protected String name;

    /**
     * Human readable name of the Truffle language
     */
    protected String humanReadableName;

    /**
     * Adjustment value for the TruffleClassInformation weights
     */
    @Relationship(type = "WEIGHT_ADJUST")
    protected Map<SystemInformation, Double> weightAdjustment = new HashMap<>();

    /**
     * Adjustment value for the TruffleClassInformation weights
     */
    @Relationship(type = "WEIGHT_UNOPTIMIZED_ADJUST")
    protected Map<SystemInformation, Double> weightUnoptimizedAdjustment = new HashMap<>();

    // endregion

    // region helper-functions

    /**
     * Adds a class to a list in a map and initializes the list or position if needed
     *
     * @param map    Map class is added to
     * @param parent parent of class (key)
     * @param clazz  value added to list of values
     */
    private void addToMap(Map<Class, List<Class>> map, Class parent, Class clazz) {
        if (clazz == null) {
            return;
        }
        if (map.containsKey(parent)) {
            if (!map.get(parent).contains(clazz)) {
                map.get(parent).add(clazz);
            }
        } else {
            map.put(parent, new ArrayList<>());
            map.get(parent).add(clazz);
        }
    }

    //endregion

    // region class hierarchy

    /**
     * Checks if a class is terminal by checking its constructors or a static create method
     *
     * @param clazz class that is being checked
     * @return if the class is terminal
     */
    private boolean isTerminal(Class clazz) {
        TruffleClassInformation tci = this.nodeClasses.getOrDefault(clazz, null);
        if (tci != null) {
            return tci.getInitializers().stream().noneMatch(x -> Arrays.stream(x.getParameters()).anyMatch(p -> this.nodeClasses.keySet().stream().anyMatch(val -> p.getClazz().isAssignableFrom(val))));
        } else {
            Logger.log(Logger.LogLevel.ERROR, "Truffle Language should have class information for this");
        }
        return false;
    }

    /**
     * Fills {@link #instantiableClasses} and {@link #instantiableTerminalClasses} by discovering the hierarchy bottom up
     *
     * @param classes all classes that can be instantiated (are not abstract)
     */
    private void discoverClassHierachy(List<Class> classes, boolean slim) {
        // first pass, just determine which we can actually instantiate
        classes.forEach(x -> {
            TruffleClassInformation tci = TruffleClassInformation.informationForClass(this, x);
            if (!slim) {
                tci.loadMetadata();
            }
            nodeClasses.put(x, tci);
            if (tci.isInstantiable()) {
                instantiableNodeClasses.put(x, tci);
            } else {
                unreachableClasses.put(x, "Class is not instantiable");
            }
        });
        // second pass determine terminality and add hierarchy classes as well
        classes.forEach(x -> {
            boolean isTerminal = isTerminal(x);

            Class parent = x;
            Class prevParent = null;
            while (parent != null) {
                // add to hierarchy counter map
                addToMap(classHierarchy, parent, prevParent);
                Class finalPrevParent = prevParent;
                Arrays.stream(parent.getInterfaces()).forEach(i -> {
                    addToMap(classHierarchy, InterfaceMarker.class, i);
                    addToMap(classHierarchy, i, finalPrevParent);
                });

                // add instantiation hierarchy
                addToMap(instantiableClasses, parent, x);
                // also add to possibly implemented interfaces
                Arrays.stream(parent.getInterfaces()).forEach(i -> addToMap(instantiableClasses, i, x));
                // if terminal also add to terminal map
                if (isTerminal) {
                    if (!terminalNodeClasses.contains(x)) {
                        terminalNodeClasses.add(x);
                    }
                    addToMap(instantiableTerminalClasses, parent, x);
                    Arrays.stream(parent.getInterfaces()).forEach(i -> addToMap(instantiableTerminalClasses, i, x));
                }
                // move up in hierarchy
                prevParent = parent;
                parent = parent.getSuperclass();
            }

            // second pass for enclosingHierarchy
            parent = x;
            prevParent = null;
            while (parent != null) {
                // add to hierarchy counter map
                addToMap(enclosingHierarchy, parent, prevParent);

                // move up in hierarchy preferring the enclosing class
                prevParent = parent;
                parent = parent.getAnnotation(GeneratedBy.class) != null ? parent.getSuperclass() : (parent.getEnclosingClass() != null ? parent.getEnclosingClass() : parent.getSuperclass());
            }
        });
    }

    // endregion

    public TruffleClassInformation getClass(String name) {
        Class clazz = ClassLoadingHelper.loadClassByName(name);
        if (clazz != null && nodeClasses.containsKey(clazz)) {
            return nodeClasses.get(clazz);
        }
        return null;
    }

    public TruffleClassInformation getClass(Class clazz) {
        if (clazz != null && nodeClasses.containsKey(clazz)) {
            return nodeClasses.get(clazz);
        }
        return null;
    }


    // region constructors

    /**
     * Class info that was already loaded will not be rebuilt
     */
    @Transient
    private static Map<String, TruffleLanguageInformation> languageInformation = new HashMap<>();

    /**
     * Slim Class info that was already loaded will not be rebuilt
     */
    @Transient
    private static Map<String, TruffleLanguageInformation> slimInformation = new HashMap<>();

    /**
     * Finds the truffle language with a given id, but only returns the absolutely minimal information needed to work with it.
     * The function also does NOT access the database!
     *
     * @param id to be loaded
     * @return Truffle language Information
     */
    public static TruffleLanguageInformation getLanguageInformationMinimal(String id) {
        if (languageInformation.containsKey(id)) {
            // if already loaded just provide this one
            return languageInformation.get(id);
        } else if (slimInformation.containsKey(id)) {
            // if not check if we can return slim already loaded
            return slimInformation.get(id);
        }
        // in slim we ignore the database and the Registration, as loading directly from the classpath is way faster
        return new TruffleLanguageInformation(id, null, true, "com.oracle.truffle", "at.fh.hagenberg");
    }

    public static TruffleLanguageInformation getLanguageInformation(String id) {
        if (id == null) {
            return null;
        }
        if (languageInformation.containsKey(id)) {
            return languageInformation.get(id);
        } else {
            // attempt to find the Root language
            ClassLoadingHelper classLoadingHelper = new ClassLoadingHelper();
            classLoadingHelper.setPackages(Arrays.asList("com.oracle.truffle", "at.fh.hagenberg"));
            classLoadingHelper.setExcludes(Arrays.asList("com.oracle.truffle.polyglot", "com.oracle.truffle.api", "com.oracle.truffle.tck", "gce.optimization", "gce.science", "gce.benchmark", "gce.lang.optimization", "machinelearning.analytics.graph"));
            classLoadingHelper.setParentClasses(Collections.singletonList(TruffleLanguage.class));

            // Find all registered languages
            List<Class> classes = classLoadingHelper.findClasses();
            Class rootClass = classes.stream().filter(x -> {
                TruffleLanguage.Registration registration = (TruffleLanguage.Registration) x.getAnnotation(TruffleLanguage.Registration.class);
                if (registration != null) {
                    return registration.id().equals(id);
                }
                return false;
            }).findFirst().orElse(null);
            if (rootClass != null) {
                TruffleLanguage.Registration registration = (TruffleLanguage.Registration) rootClass.getAnnotation(TruffleLanguage.Registration.class);
                // Note: we simply assume that the language class is 1 package below all other nodes. This currently holds true for all supported languages but may not in the future
                return new TruffleLanguageInformation(id, registration.name(), false, rootClass.getPackage().getName().substring(0, rootClass.getPackage().getName().lastIndexOf('.')));
            }
        }
        // if nothing worked just collect all nodes randomly and see what happens:
        return new TruffleLanguageInformation(id, null, false, "com.oracle.truffle", "at.fh.hagenberg");
    }

    /**
     * initializer for DB. DO NOT USE FOR ANYTHING ELSE
     */
    public TruffleLanguageInformation() {
    }

    /**
     * Function for DB adding loaded language to cache. DO NOT USE OTHERWISE
     */
    public void setSelf() {
        languageInformation.put(name, this);
    }

    /**
     * Automatically instantiates the class using all Node objects.
     *
     * @param name              Truffle Language ID
     * @param humanReadableName Human readable name (usually truffle language name)
     * @param slim              if the language should be calculated in a minimalistic way
     * @param packageName       Name of (parent) package that nodes must be in
     */
    protected TruffleLanguageInformation(String name, String humanReadableName, boolean slim, String... packageName) {
        this.name = name;
        this.humanReadableName = humanReadableName;

        // store for re-use
        if (!slim) {
            languageInformation.put(name, this);
        } else {
            slimInformation.put(name, this);
        }

        try {
            ClassLoadingHelper classLoadingHelper = new ClassLoadingHelper();
            classLoadingHelper.setExcludes(Arrays.asList("com.oracle.truffle.polyglot", "com.oracle.truffle.api", "com.oracle.truffle.tck", "gce.optimization", "gce.science", "gce.benchmark", "gce.lang.optimization"));
            classLoadingHelper.setParentClasses(Collections.singletonList(Node.class));
            if (packageName != null) {
                for (int i = 0; i < packageName.length; i++) {
                    packageName[i] = packageName[i].replace(".", "/");
                }
                classLoadingHelper.setPackages(Arrays.asList(packageName));
            }
            classLoadingHelper.setRealOnly(true);
            List<Class> realClasses = classLoadingHelper.findClasses();
            discoverClassHierachy(realClasses, slim);

            // ensure that we initialize the minimal depth of our class information
            int uninitializedPreviously = 0;
            AtomicInteger uninitialized = new AtomicInteger(nodeClasses.size());
            while (uninitialized.get() != 0 && uninitialized.get() != uninitializedPreviously) {
                uninitializedPreviously = uninitialized.get();
                uninitialized.set(0);
                nodeClasses.values().forEach(x -> {
                    int i = x.getMinimalSubtreeSize();
                    if (i < 0) {
                        uninitialized.getAndIncrement();
                    }
                });
            }

            // remove all classes that can't determine their minimal depth, as they are missing related nodes and can't be instantiated
            new HashSet<>(instantiableNodeClasses.entrySet()).stream().filter(x -> x.getValue().getMinimalSubtreeSize() < 0).forEach(x -> {
                unreachableClasses.put(x.getKey(), "Class is missing related node class");
                instantiableNodeClasses.remove(x.getKey());
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the language information was learned, and posesses all necessary data for THIS system
     *
     * @return
     */
    public boolean learned() {
        return this.getInstantiableNodes().values().stream().allMatch(x -> x.hasProperty(TruffleClassProperty.TRUFFLE_BOUNDARY) || x.getWeight().containsKey(SystemInformation.getCurrentSystem()));
    }

// endregion

    // region Getters Setters

    @Override
    public Map<Class, List<Class>> getOperators() {
        return instantiableClasses;
    }

    @Override
    public Map<Class, List<Class>> getOperands() {
        return instantiableTerminalClasses;
    }

    public Map<Class, TruffleClassInformation> getNodes() {
        return nodeClasses;
    }

    @Override
    public Map<Class, TruffleClassInformation> getInstantiableNodes() {
        return instantiableNodeClasses;
    }

    public Map<Class, String> getUnreachableClasses() {
        return unreachableClasses;
    }

    public Map<Class, List<Class>> getClassHierarchy() {
        return classHierarchy;
    }

    public Map<Class, List<Class>> getEnclosingHierarchy() {
        return enclosingHierarchy;
    }

    public String getName() {
        return name;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    public Long getId() {
        return id;
    }

    public TruffleClassInformation getTci(NodeWrapper tree) {
        return getTci(tree.getType());
    }

    public TruffleClassInformation getTci(String className) {
        Class clazz = getNodes().keySet().stream().filter(x -> x.getName().equals(className)).findFirst().orElseGet(null);
        return clazz != null ? getTci(clazz) : null;
    }

    public TruffleClassInformation getTci(Class clazz) {
        return getNodes().getOrDefault(clazz, null);
    }

    public TruffleClassInformation getTci(long id) {
        return getNodes().values().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
    }

    protected Map<SystemInformation, Double> getWeightAdjustment() {
        return weightAdjustment;
    }

    protected void setWeightAdjustment(Map<SystemInformation, Double> weightAdjustment) {
        this.weightAdjustment = weightAdjustment;
    }

    protected Map<SystemInformation, Double> getWeightUnoptimizedAdjustment() {
        return weightUnoptimizedAdjustment;
    }

    protected void setWeightUnoptimizedAdjustment(Map<SystemInformation, Double> weightUnoptimizedAdjustment) {
        this.weightUnoptimizedAdjustment = weightUnoptimizedAdjustment;
    }


    // endregion
}
