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

import science.aist.seshat.Logger;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Helper class that allows us to find all classes in the classpath(s)
 * Note: This is a modification of the code in
 * <p>
 * Warning: Restricting to packages is STRONGLY recommended, as this loads all classes otherwise
 *
 * @author Oliver Krauss on 03.10.2019
 */
public class ClassLoadingHelper {

    /**
     * Packages that classes must be in (sub-packages allowed too!)
     * If null all packages will be loaded.
     */
    private List<String> packages = null;

    /**
     * Classes that the loaded classes MUST extend from (sub-extensions allowed too!)
     * Note that the classes in parentClasses will never be returned by findClasses()
     */
    private List<Class> parentClasses = null;

    private List<String> paths = graalPaths;

    private List<String> excludes = new LinkedList<>();

    private List<String> defaultExcludes = Arrays.asList("com/oracle/truffle/object", "META-INF/versions");

    /**
     * If true only classes that are NOT abstract will be returned
     */
    private boolean realOnly = false;

    private static Logger logger = Logger.getInstance();

    public ClassLoadingHelper() {
    }

    private static final List<String> graalPaths = findPaths();

    private static List<String> findPaths() {
        // find all classes in classpath
        String classpath = System.getProperty("java.class.path");
        // also find all classes in truffle path
        String trufflePath = System.getProperty("truffle.class.path.append");
        if (trufflePath != null) {
            classpath += System.getProperty("path.separator") + trufflePath;
        }
        List<String> paths = new LinkedList<>(Arrays.asList(classpath.split(System.getProperty("path.separator"))));

        // remove default dependencies that we know have nothing of interest (for speed)
        paths.removeIf(x -> x.contains("jre/lib")  // prevent JRE libraries
                || x.contains("/JetBrains/")  // prevent IntelliJ
                || (x.contains("/.m2/") && !x.contains("/truffle/")) // remove all dependencies except truffle
                || x.contains("/truffle-dsl") || x.contains("/truffle-tck") // clear truffle dsl and api
                || x.contains("/test-classes") // prevent anything from tests
        );

        // remove self from loading, to prevent recursive initialization of framework by itself
        paths.removeIf(x -> x.contains("optimization-external"));

        return paths;
    }

    public ClassLoadingHelper(List<String> packages) {
        setPackages(packages);
    }


    public List<Class> findClasses() {
        try {
            // find in classpath (jar + directories) and remove to only in packages we need
            List<Class> availableClasses = new ArrayList<>();
            for (String path : paths) {
                File base = new File(path);
                if (base.isDirectory()) {
                    // check unpacked files
                    availableClasses.addAll(findClassesInDirectory(base));
                } else if (base.isFile() && base.getName().endsWith(".jar")) {
                    // check jar
                    JarFile file = new JarFile(base);
                    Enumeration<JarEntry> entries = file.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (isRequiredClass(entry.getName())) {
                            Class x = loadClass(entry.getName());
                            if (x != null) {
                                availableClasses.add(x);
                            }
                        }
                    }
                }
            }

            //reduce to classes that implement "classes"
            if (parentClasses != null) {
                List<Class> languageClasses = new ArrayList<>();
                boolean found = true;
                while (found) {
                    found = false;
                    availableClasses.removeAll(languageClasses);
                    for (Class clazz : availableClasses) {
                        if (!Modifier.isPrivate(clazz.getModifiers()) &&
                                (languageClasses.contains(clazz.getSuperclass())
                                        || parentClasses.stream().anyMatch(x -> clazz != x && x.isAssignableFrom(clazz))
                                        || parentClasses.stream().anyMatch(x -> Arrays.stream(clazz.getInterfaces()).anyMatch(i -> x.isAssignableFrom(i))))) {
                            languageClasses.add(clazz);
                            found = true;
                        }
                    }
                }
                availableClasses = languageClasses;
            }

            if (realOnly) {
                availableClasses = availableClasses.stream().filter(x -> !Modifier.isAbstract(x.getModifiers())).collect(Collectors.toList());
            }

            return availableClasses;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * safely loads class, skips classes that can't be loaded
     *
     * @param path path to class
     * @return class or null
     */
    private Class loadClass(String path) {
        if (path == null || path.contains("Main.class") || path.contains("ApplicationContextProvider") || path.contains("GceExternalOptimizationWorker")
            || path.contains("EpmPublication.class") || path.contains("TrufflePublicAccess.class")) {
            // we don't load "Main" classes as they initialize the spring context and that takes too much time.
            // We also don't load the ApplicationContextProvider or classes using it for the same reason
            return null;
        }
        try {
            int start = path.contains("classes") ? path.indexOf("classes") + 8 : 0;
            return Class.forName(path.substring(start, path.lastIndexOf(".")).replace('/', '.'));
        } catch (Exception | Error e) {
            logger.warn("Could not load " + path);
        }
        return null;
    }

    public static Class loadClassByName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            logger.warn("Could not load " + name);
        }
        return null;
    }

    /**
     * Finds all classes in a directory
     *
     * @param f folder that is being searched
     */
    private List<Class> findClassesInDirectory(File f) {
        List<Class> classes = new ArrayList<>();
        for (File file : f.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(file));
            } else if (isRequiredClass(file.getPath())) {
                Class x = loadClass(file.getPath());
                if (x != null) {
                    classes.add(x);
                }
            }
        }
        return classes;
    }

    /**
     * Helper function that checks if a file is a class and is in the correct package
     *
     * @param path path to search
     * @return if file is relevant
     */
    private boolean isRequiredClass(String path) {
        return path.endsWith(".class") && defaultExcludes.stream().noneMatch(path::contains) && excludes.stream().noneMatch(path::contains) && (packages == null || packages.stream().anyMatch(path::contains));
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = new LinkedList<>();
        packages.forEach(x -> this.packages.add(x.replace(".", "/")));
    }

    public List<Class> getParentClasses() {
        return parentClasses;
    }

    public void setParentClasses(List<Class> parentClasses) {
        this.parentClasses = parentClasses;
    }

    public boolean isRealOnly() {
        return realOnly;
    }

    public void setRealOnly(boolean realOnly) {
        this.realOnly = realOnly;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = new LinkedList<>();
        excludes.forEach(x -> this.excludes.add(x.replace(".", "/")));
    }
}
