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
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import com.oracle.truffle.api.nodes.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing the metadata of a Truffle Language in order to mine hierarchical patterns.
 *
 * @author Oliver Krauss on 11.04.2019
 */
public class BitwisePatternMetaLoader {

    /**
     * Abstracts the given language
     * Note: this just does a search of the given dataType names in the class names!
     *
     * @param languageId language that shall be loaded
     * @param dataTypes  names of datatypes to be exctracted
     * @param superclass if the original hierarchy shall be loaded by superclass (true) or by enclosing class (false)
     * @return meta that has data types at the bottom of the hierarchy instead of inbetween
     */
    public static BitwisePatternMeta loadDatatypeIndependent(String languageId, List<String> dataTypes, boolean superclass) {
        Map<String, List<String>> classHierarchy = loadClassHierarchy(TruffleLanguageInformation.getLanguageInformationMinimal(languageId), superclass);
        rewrite(classHierarchy, dataTypes, Node.class.getName());
        return new BitwisePatternMeta(classHierarchy, Node.class.getName());
    }

    private static void rewrite(Map<String, List<String>> classHierarchy, List<String> dataTypes, String root) {
        List<String> current = classHierarchy.get(root);
        if (current == null) {
            // reached floor
            return;
        }
        // collect all classes containing the DT name AND still being an abstract class (we must not remove physical classes from the hierarchy)
        List<String> dataTypeDependent = current.stream().filter(x -> dataTypes.stream().anyMatch(x::contains) && classHierarchy.containsKey(x)).collect(Collectors.toList());

        current.removeAll(dataTypeDependent);
        // combine all data type dependent nodes
        while (!dataTypeDependent.isEmpty()) {
            // combine in one name
            String abstracted = replaceName(dataTypeDependent.get(0), dataTypes);

            // find all to combine
            List<String> combine = dataTypeDependent.stream().filter(x -> abstracted.equals(replaceName(x, dataTypes))).collect(Collectors.toList());
            dataTypeDependent.removeAll(combine);

            // add new object to hierarchy and remove old ones
            current.add(abstracted);
            List<String> combinedChildren = combine.stream().flatMap(x -> classHierarchy.get(x).stream()).collect(Collectors.toList());
            combine.forEach(classHierarchy::remove);
            classHierarchy.put(abstracted, combinedChildren);
        }

        current.forEach(x -> rewrite(classHierarchy, dataTypes, x));
    }

    private static String replaceName(String original, List<String> dataTypes) {
        // remove package name
        original = original.substring(original.lastIndexOf('.') + 1);
        // remove data type names
        for (String x : dataTypes) {
            if (original.contains(x)) {
                original = original.replace(x, "DT");
            }
        }
        return original;
    }

    public static Map<String, List<String>> loadClassHierarchy(TruffleLanguageInformation tli, boolean superclass) {
        Map<Class, List<Class>> classHierarchy = superclass ? tli.getClassHierarchy() : tli.getEnclosingHierarchy();
        Map<String, List<String>> stringMap = new HashMap<>();

        classHierarchy.forEach((k, v) -> {
            List<String> classes = new LinkedList<>();
            v.forEach(x -> classes.add(x.getName()));
            stringMap.put(k.getName(), classes);
        });

        return stringMap;
    }

}
