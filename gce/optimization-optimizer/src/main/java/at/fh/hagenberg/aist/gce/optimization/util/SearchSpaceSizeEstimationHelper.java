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

import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;

import java.lang.reflect.*;
import java.util.*;

/**
 * Created by Oliver Krauss on 15.02.2017.
 */
public class SearchSpaceSizeEstimationHelper {

    /**
     * Helper function that estimates how many different trees can be created with the current settings
     *
     * @return estimate
     */
    public long estimateAllSolutions(CreationConfiguration configuration, TruffleLanguageSearchSpace searchSpace) {
        long estimate = 0;

        for (Class nodeClass : searchSpace.getInstantiableNodes().keySet()) {
            long est = estimatePossibleSolutions(nodeClass, configuration.getMaxDepth() + 1, configuration, searchSpace); // +1 as the generators don't count terminals as a level
            estimate += est >= 0 ? est : 0;
        }

        return estimate;
    }

    /**
     * Cache so we don't have to re-visit valid subtrees
     */
    private HashMap<String, Long> estimationCache = new HashMap<>();

    /**
     * Counts how often a subtree occurs
     */
    private HashMap<String, Long> estimationOccurence = new HashMap<String, Long>();

    /**
     * Calculates the factorial of a number (ex. 5! = 120)
     *
     * @param number before !
     * @return ! of number
     */
    private static long factorial(int number) {
        long result = 1;

        for (int factor = 2; factor <= number; factor++) {
            result *= factor;
        }

        return result;
    }

    /**
     * Helper function that estimates all valid subtrees of a specific class
     *
     * @param nodeClass  to be instantiated
     * @param maxDescent how deep the recursion is allowed to be
     * @return estimate of different trees that can be created by this class
     */
    private long estimatePossibleSolutions(Class nodeClass, int maxDescent, CreationConfiguration configuration, TruffleLanguageSearchSpace information) {
        if (maxDescent < 1) {
            // 0 -> no options possible
            return -1;
        }

        // check for terminals
        switch (nodeClass.getName()) {
            case "int":
                return 4;
            case "char":
                return 69;
            case "double":
                return 11;
            case "float":
                return 11;
            case "boolean":
                return 2;
            case "java.lang.String":
                return 2;
            case "com.oracle.truffle.api.frame.MaterializedFrame":
                return 1;
            case "com.oracle.truffle.api.frame.FrameSlot":
                return 1;
            default: {
                if (maxDescent == 1) {
                    // at a depth of one we can only have terminals
                    return -1;
                }
                break;
            }
        }

        String key = nodeClass.getName() + maxDescent;
        if (estimationCache.containsKey(key)) {
            if (!estimationOccurence.containsKey(key)) {
                estimationOccurence.put(key, 1L);
            }
            estimationOccurence.put(key, estimationOccurence.get(key) + 1);

            return estimationCache.get(key);
        }

        // needed so often we decrease depth here
        maxDescent = maxDescent - 1;

        // Check for arrays
        if (nodeClass.isArray()) {
            long subSolutions = estimatePossibleSolutions(nodeClass.getComponentType(), maxDescent, configuration, information);
            long result = subSolutions < 0 ? -1 : factorial(configuration.getMaxDepth()) * subSolutions;
            estimationCache.put(key, result);
            return result;
        }

        // In case of abstract classes we want ALL real implementations of this class
        if (Modifier.isAbstract(nodeClass.getModifiers())) {
            int subSolutions = 1;
            if (information.getOperators().get(nodeClass) != null) {
                for (Class option : information.getOperators().get(nodeClass)) {
                    // count all solutions that are valid (-1 can be returned in case of nodes that are too big)
                    long subSolution = estimatePossibleSolutions(option, maxDescent, configuration, information);
                    subSolutions *= subSolution >= 0 ? subSolution : 1;
                }
                // if no valid solutions found, this is also an invalid solution
                long result = subSolutions > 0 ? subSolutions : -1;
                estimationCache.put(key, result);
                return result;
            } else {
                Logger.log(Logger.LogLevel.INFO, "ERROR Class " + nodeClass.getName() + " not known");
                return 1;
            }
        }

        int paramOptions = 1;
        TruffleClassInformation initializer = information.getInstantiableNodes().get(nodeClass);
        if (initializer == null) {
            return 1;
        }

        if (initializer.getInitializers().size() == 0) {
            estimationCache.put(key, 0L);
            return 0L;
        }
        for (int i = 0; i < initializer.getInitializers().get(0).getParameters().length; i++) {
            long paramOption = estimatePossibleSolutions(initializer.getInitializers().get(0).getParameters()[i].getType(), maxDescent, configuration, information);
            paramOptions *= paramOption >= 0 ? paramOption : 1;
        }
        long result = initializer.getInitializers().get(0).getParameters().length * (paramOptions > 0 ? paramOptions : -1);
        estimationCache.put(key, result);
        return result;
    }

}
