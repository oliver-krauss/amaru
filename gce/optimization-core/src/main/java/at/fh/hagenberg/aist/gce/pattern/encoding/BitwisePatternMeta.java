/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.encoding;

import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.InterfaceMarker;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import com.oracle.truffle.api.nodes.Node;

import java.util.*;
import java.util.function.IntSupplier;

/**
 * Class representing the metadata of a Truffle Language in order to mine hierarchical patterns.
 *
 * @author Oliver Krauss on 11.04.2019
 */
public class BitwisePatternMeta {

    private static final String API_NODE = "GRAAL_API";

    private static final String EXCLUDED_NODE = "OTHER";

    /**
     * Truffle Language that the meta info represents
     */
    private TruffleLanguageInformation tli;

    /**
     * Map of class to bitmask
     * Ex.: IntNode -> 10000000
     * Ex.: IntLiteralNode -> 10000100 (also includes the parent mask!!!)
     */
    private Map<String, Long> bitmask = new HashMap<>();

    /**
     * Map of bitmask to class
     * Ex.: 10000000 -> IntNode
     * Ex.: 10000100 -> IntLiteralNode
     */
    private Map<Long, String> bitmaskInverse = new HashMap<>();

    /**
     * Defines which layer ends at which position
     * Ex.: 0 -> 6  [meaning bits 0 through 5]
     * Ex.: 1 -> 14 [meaning bits 6 through 13]
     * Ex.: 3 -> 21 [meaning bits 14 through 21]
     */
    private Map<Integer, Integer> bitlayer = new HashMap<>();

    private Map<String, List<String>> classHierarchy;

    /**
     * Default pattern meta created from the class hierarchy of the tli
     *
     * @param tli        - language information that the hierarchy shall be created from
     * @param superclass - if true will be created fom tli.classHierarchy,
     *                   if false will be created from the enclosing classes (logical structure designed by the developer)
     */
    public BitwisePatternMeta(TruffleLanguageInformation tli, boolean superclass) {
        this(BitwisePatternMetaLoader.loadClassHierarchy(tli, superclass), Node.class.getName());
        this.tli = tli;
    }

    /**
     * Default pattern meta created from the class hierarchy of the tli
     *
     * @param tli - language information that the hierarchy shall be created from
     */
    public BitwisePatternMeta(TruffleLanguageInformation tli) {
        this(tli, true);
    }

    /**
     * This is a dummy initializer that creates a flat meta for all classes given
     * It essentially is 2 layered:
     * 0 -> 0  Simply exists to support the . Wildcard (e.g. we don't care what class is here)
     * 1 -> ?  All other classes in a single layer
     *
     * @param classes
     */
    public BitwisePatternMeta(List<String> classes) {
        this(dummyMap(classes), Wildcard.WILDCARD_ANY_NODE);
    }

    /**
     * Helper class transforming list to Map
     *
     * @param classes to be mapped
     * @return hierarchical map including a root
     */
    private static Map<String, List<String>> dummyMap(List<String> classes) {
        Map<String, List<String>> dummyHierarchy = new HashMap<>();
        dummyHierarchy.put(Wildcard.WILDCARD_ANY_NODE, classes);
        return dummyHierarchy;
    }

    /**
     * This an initializer for custom made hierarchies.
     *
     * @param classHierarchy hierarchy that shall be represented by the meta
     * @param root           class that will be at the root of the hierarchy
     */
    public BitwisePatternMeta(Map<String, List<String>> classHierarchy, String root) {
        if (classHierarchy.containsKey("java.lang.Object") && classHierarchy.get("java.lang.Object").contains("at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode")) {
            // TODO #63 this is a manual remap so we don't change the MiniC language between baseline and kggi.
            // simpleliteralnode, readfunctionarg and castnode SHOULD all three be extending from MiniC node to be auto-mapped
            classHierarchy.get("java.lang.Object").remove("at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode");
            classHierarchy.get("java.lang.Object").remove("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNode");
            classHierarchy.get("java.lang.Object").remove("at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicCastNode");
            classHierarchy.get("at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode").add("at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode");
            classHierarchy.get("at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode").add("at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNode");
            classHierarchy.get("at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode").add("at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicCastNode");
        }

        // Entry point is Object, as some classes have multiple roots
        if (classHierarchy.containsKey(root)) {
            this.classHierarchy = classHierarchy;
            int level = 0;

            // . wildcard
            bitlayer.put(-1, 0);
            bitmask.put(root, 0L);
            bitmaskInverse.put(0L, root);

            // * wildcard
            bitmask.put(Wildcard.WILDCARD_ANYWHERE, Long.MAX_VALUE);
            bitmaskInverse.put(Long.MAX_VALUE, Wildcard.WILDCARD_ANYWHERE);

            // API Nodes
            classHierarchy.get(root).add(API_NODE);
            classHierarchy.get(root).add(EXCLUDED_NODE);

            List<String> entryPoints = new LinkedList<>();
            entryPoints.add(root);
            List<String> childClasses = new ArrayList<>();
            while (!entryPoints.isEmpty()) {
                // go through all entry points and assign bitmask values
                int largestGroup = entryPoints.stream().filter(classHierarchy::containsKey).mapToInt(x -> classHierarchy.get(x).size()).max().orElse(0);
                if (largestGroup == 0) {
                    // when largest group is 0 we are DONE
                    bitlayer.remove(-1);
                    return;
                }
                // inc largest group by 1, as we have to start with 1 to ensure differences in the child masks
                largestGroup += 1;

                // determine size of mask
                int maskSize = 1;
                while (Math.pow(2, maskSize) < largestGroup) {
                    maskSize++;
                }
                bitlayer.put(level, maskSize + bitlayer.get(level - 1));
                level++;

                // assign masks
                for (String x : entryPoints) {
                    long mask = 1;
                    // assign bitmasks
                    List<String> classes = classHierarchy.get(x);

                    // we can skip classes with only 1 child IF their children don't suddenly create the new "largest group"
                    List<String> skippable = new LinkedList<>();
                    while (classes != null && classes.size() == 1) {
                        skippable.add(classes.get(0));
                        Logger.log(Logger.LogLevel.DEBUG, "Skipping Class " + skippable);
                        if (classHierarchy.containsKey(classes.get(0)) && classHierarchy.get(classes.get(0)).size() < largestGroup) {
                            classes = classHierarchy.get(classes.get(0));
                        } else {
                            classes = null;
                            skippable.forEach(y -> {
                                        bitmask.put(y, bitmask.get(x));
                                        bitmaskInverse.put(bitmask.get(x), y);
                                    }
                            );
                        }
                    }

                    if (classes != null && !classes.isEmpty()) {
                        for (String clazz : classes) {
                            if (clazz != null) {
                                try {
                                    // generate mask
                                    String superclazz = x;
                                    long newMask = (mask << (64 - bitlayer.get(level - 1))) +
                                            (bitmask.containsKey(superclazz) ? bitmask.get(superclazz) : 0);

                                    // sanity check
                                    if (bitmask.containsKey(clazz) || bitmaskInverse.containsKey(newMask)) {
                                        throw new RuntimeException("The bitmask already exists. We have made an error in calculating the masking");
                                    }

                                    // add bitmask
                                    bitmask.put(clazz, newMask);
                                    bitmaskInverse.put(newMask, clazz);

                                    // add skipped classes with SAME bitmask (not inverse, as the mask must be unique there)
                                    while (!skippable.isEmpty()) {
                                        String skipped = skippable.remove(0);
                                        bitmask.put(skipped, newMask);
                                    }

                                    Logger.log(Logger.LogLevel.TRACE, String.format("%64s", Long.toBinaryString(bitmask.get(clazz))).replace(' ', '0'));
                                    mask++;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        childClasses.addAll(classes);
                    }
                }


                entryPoints = childClasses;
                childClasses = new ArrayList<>();
            }

            bitlayer.remove(-1);
        } else {
            throw new RuntimeException("Selected Root does not exist!");
        }
    }

    /**
     * Produces a Bitmask from a given class
     *
     * @param clazz to be masked
     */
    public Long mask(Class<?> clazz) {
        return this.bitmask.getOrDefault(clazz.getName(), null);
    }

    /**
     * Produces a Bitmask from a given class
     *
     * @param clazz to be masked
     */
    public Long mask(String clazz) {
        return this.bitmask.getOrDefault(clazz, ExtendedNodeUtil.isAPINode(clazz) ? this.bitmask.getOrDefault(API_NODE, this.bitmask.get(EXCLUDED_NODE)) : this.bitmask.get(EXCLUDED_NODE));
    }

    /**
     * Returns the class of the bitmask
     *
     * @param clazz to be unmasked
     */
    public String unmask(Long clazz) {
        return this.bitmaskInverse.getOrDefault(clazz, null);
    }

    /**
     * Returns all classes in the hierarchy of the given bitmask
     *
     * @param clazz to get hierarchy of
     */
    public String[] hierarchy(Class clazz) {
        return hierarchy(clazz.getName());
    }

    /**
     * Returns all classes in the hierarchy of the given bitmask
     * IF this is a Graal/Truffle class that is not handled in the language it will be returned AS the hierarchy
     *
     * @param clazz to get hierarchy of
     */
    public String[] hierarchy(String clazz) {
        if (ExtendedNodeUtil.isAPINode(clazz) || !this.bitmask.containsKey(clazz)) {
            return new String[]{clazz};
        }
        return hierarchy(this.bitmask.getOrDefault(clazz, null));
    }

    /**
     * Returns all classes in the hierarchy of the given bitmask
     *
     * @param clazz to get hierarchy of
     */
    public String[] hierarchy(Long clazz) {
        if (clazz == null) {
            return null;
        }

        // determine how many layers we need to load
        int size = bitlayer.size() - 1;
        while (size > 0 && (clazz << bitlayer.get(size)) == 0) {
            // bitmask becomes 0 if we shift all "1" bits away
            size--;
        }
        // ensure last layer is also considered in case of directly below root
        if ((clazz << bitlayer.get(size)) == 0) {
            size--;
        }
        // size added by 1 as the final shift determined the actual layer size.
        // (and another 1 as we start from 0 index, and another 1 for the "root" wildcard)
        String[] hierarchy = new String[++size + 2];

        hierarchy[0] = this.bitmaskInverse.get(0L);
        for (int i = 0; i <= size; i++) {
            Integer masksize = bitlayer.getOrDefault(i, 0);
            hierarchy[i + 1] = this.bitmaskInverse.get(((clazz >> (64 - masksize)) << (64 - masksize)));
        }

        return hierarchy;
    }

    /**
     * Collects all bottom level classes for the given class (e.g. descends through all hierarchy layers).
     * Use this if you want to know what the given hierarchy encapsulates.
     *
     * @param clazz to be checked
     * @return all instantiables of the given class
     */
    public List<String> instantiables(String clazz) {
        List<String> instantiables = new ArrayList<>();

        if (!classHierarchy.containsKey(clazz)) {
            return new ArrayList<>(Collections.singleton(clazz));
        }
        List<String> check = new ArrayList<>(classHierarchy.get(clazz));

        while (!check.isEmpty()) {
            String curr = check.remove(0);
            if (classHierarchy.containsKey(curr)) {
                check.addAll(classHierarchy.get(curr));
            } else {
                instantiables.add(curr);
            }
        }

        return instantiables;
    }

    public int maxHeight() {
        return this.bitlayer.size();
    }

    /**
     * returns the size of the mask this class has
     *
     * @param clazz size of mask for class
     */
    public long maskSize(long clazz) {
        // determine how many layers we need to load
        int size = bitlayer.size() - 2;
        while (size > 0 && (clazz << bitlayer.get(size)) == 0) {
            // bitmask becomes 0 if we shift all "1" bits away
            size--;
        }
        return bitlayer.get(size + 1);
    }
}
