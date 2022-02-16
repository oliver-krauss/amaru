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

import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestComplexity;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleTestValue;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Truffle Function Analyzer is responsible for creating a function signature, e.g. which arguments a function takes
 * <p>
 * Currently it can detect:
 * - The total amount of arguments (max, some can be optional in between!)
 * - The type of the argument as java class
 * <p>
 * The detection can be done via:
 * - Analyzing the source code (what arguments are being accessed with what type)
 * --- Limitation: Currently only nodes with a single argument type are inferrable (ex. reads "int" is ok; reads "int" and "short" is ambiguous and can't be inferred)
 * - Analyzing the test data being input into the function
 * <p>
 * Not yet implemented:
 * - Function Output as we currently have no need for that
 */
public class TruffleFunctionAnalyzer {

    /**
     * Retrieves the signature of the function by analyzing the source code
     *
     * @param node        to be analyzed
     * @param information the language to be analyzed in
     * @return singature of the function
     */
    public static TruffleFunctionSignature getSignature(RootNode node, TruffleLanguageInformation information) {
        if (node == null) {
            return null;
        }

        // collect nodes
        Stream<Node> nodes = ExtendedNodeUtil.flatten(node);
        Map<Integer, String> typeMap = new HashMap<>();

        // check all our nodes for reads
        nodes.forEach(x -> {
            TruffleClassInformation tci = information.getClass(x.getClass());
            if (tci != null && tci.hasProperty(TruffleClassProperty.STATE_READ_ARGUMENT)) {
                // we found an argument node

                // infer type
                String type = null;
                if (tci.getArgumentReadClasses().size() == 1) {
                    // if type is definite we are ok. Otherwise it is "unknown"
                    type = tci.getArgumentReadClasses().get(0);
                } else if (x.getParent() != null && x.getParent().getClass().getName().contains("CopyGenericArrayNode")) {
                    type = "array";
                } else {
                    // manual overrides for minic builtins
                    switch (node.getName()) {
                        case "print":
                            type = "object";
                            break;
                        case "sqrt":
                        case "powf":
                        case "exp":
                            type = "float";
                            break;
                        case "length":
                            type = "string";
                            break;
                        default:
                            Logger.log(Logger.LogLevel.INFO, "failed to determine an argument type for function " + node.getName());
                    }
                }

                // infer position
                Integer position = 0;
                /**
                 * This is admittedly super hacky. Most languages don't allow a read from a specific argument position.
                 * Most languages in truffle also name this field "index" or "argIndex".
                 * So this is how we match. Extract the field, and its value.
                 *
                 * IF this seems unfeasible in the future we have a better option (Look at TruffleLanguageLearner):
                 * We create an empty pseudo function, inject a copy of node "x", create a dummy "type"
                 *   and put it at increasing function argument positions until the code stops failing.
                 * Also hacky, but should work even on edge case nodes.
                 */
                List<Field> fields = JavaAssistUtil.findFieldFuzzy("index", x.getClass());
                if (fields.size() != 1) {
                    throw new RuntimeException("The find argument index hack doesn't work with class " + x.getClass() + " time to upgrade the code.");
                } else {
                    Object o = JavaAssistUtil.safeFieldAccess(fields.get(0), x);
                    if (o == null || !(o instanceof Integer)) {
                        throw new RuntimeException("The find argument index hack found something, but it probably wasn't the index in class " + x.getClass() + " time to upgrade the code.");
                    } else {
                        position = (Integer) o;
                    }
                }

                // validate
                if (!typeMap.containsKey(position)) {
                    typeMap.put(position, type);
                } else if (type != null && !typeMap.get(position).equals(type)) {
                    throw new RuntimeException("The test data seems to be incorrect. Argument at " + position + " can't be " + typeMap.get(position) + " and " + type);
                }
            }
        });

        // map to array
        String[] arguments = new String[typeMap.size()];
        for (int i = 0; i < typeMap.size(); i++) {
            arguments[i] = typeMap.get(i);
        }

        TruffleFunctionSignature signature = new TruffleFunctionSignature(node.getCallTarget(), arguments);
        signatureMap.put(node, signature);
        return signature;
    }

    /**
     * Retrieves the signature of the function by analyzing the test cases
     *
     * @param testCases to be analyzed
     * @return singature of the function
     */
    public static TruffleFunctionSignature getSignature(RootNode node, Set<TruffleOptimizationTestComplexity> testCases) {
        if (testCases == null || node == null) {
            return null;
        }

        // extract the input values
        List<List<TruffleTestValue>> values = testCases.stream().map(x -> x.getTest().getInput()).collect(Collectors.toList());
        String[] arguments = new String[values.stream().mapToInt(List::size).max().orElse(0)];
        values.forEach(inputGroup -> {
            Iterator<TruffleTestValue> iterator = inputGroup.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                TruffleTestValue next = iterator.next();
                if (arguments[i] == null) {
                    // add as of yet unknown type
                    arguments[i] = next.getType();
                } else if (next.getType() != null && !arguments[i].equals(next.getType())) {
                    // types are not equivalent. Testdata is probably wrong
                    throw new RuntimeException("The test data seems to be incorrect. Argument at " + i + " can't be " + arguments[i] + " and " + next.getType());
                }
                i++;
            }
        });

        TruffleFunctionSignature signature = new TruffleFunctionSignature(node.getCallTarget(), arguments);
        signatureMap.put(node, signature);
        return signature;
    }

    /**
     * Signatures of already analyzed functions
     */
    private static final Map<RootNode, TruffleFunctionSignature> signatureMap = new HashMap<>();

    private static boolean warned = false;

    /**
     * This function returns only already analyzed functions!
     *
     * @param node to get signature for
     * @return signature of function
     */
    public static TruffleFunctionSignature getSignature(RootNode node) {
        if (!signatureMap.containsKey(node)) {
            if (!warned) {
                warned = true;
                System.out.println("WARNING: Function Signature not found, that is going to be a problem");
            }
            return null;
        }
        return signatureMap.get(node);
    }
}
