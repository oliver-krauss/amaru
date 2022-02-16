/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph.nodes;

import at.fh.hagenberg.aist.gce.optimization.util.*;
import science.aist.seshat.Logger;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class helping to find all writes and reads in a function
 *
 * @author Oliver Krauss on 06.05.2020
 */
public class DataFlowUtil {

    /**
     * Logger for data flow
     */
    private static final Logger logger = Logger.getInstance();

    /**
     * Returns a map of all available data items contained in the root node until the breakoffPoint (exclusive!)
     *
     * @param information   language that will be used to find writes
     * @param rootNode      node to be mined
     * @param breakOffPoint subtree NOT searched in tree (if null, all writes will be searched), exclusive (point and its parents will not be part of search) primary use for Mutation/Crossover Points
     * @return all WRITE data items
     */
    public static Map<Object, List<DataFlowNode>> findAvailableDataItems(TruffleLanguageInformation information, Node rootNode, Node breakOffPoint) {
        return findDataItems(information, rootNode, breakOffPoint, TruffleClassProperty.STATE_WRITE);
    }

    /**
     * Returns a map of all necessary data items contained in the root node until the breakoffPoint (exclusive!)
     * Please note, if you want to mine whatever is IN the breakoff point just apply this function with the breakoffPoint as root node!
     *
     * @param information   language that will be used to find writes
     * @param rootNode      node to be mined
     * @param breakOffPoint subtree NOT searched in tree (if null, all reads will be searched), exclusive (point and its parents will not be part of search) primary use for Mutation/Crossover Points
     * @return all READ data items
     */
    public static Map<Object, List<DataFlowNode>> findRequiredDataItems(TruffleLanguageInformation information, Node rootNode, Node breakOffPoint) {
        return findDataItems(information, rootNode, breakOffPoint, TruffleClassProperty.STATE_READ);
    }

    /**
     * Returns a map of all data items that are read from but never written to beforehand
     *
     * @param information   language that will be used to find writes
     * @param rootNode      node to be mined
     * @param breakOffPoint subtree NOT searched in tree (if null, all reads will be searched), exclusive (point and its parents will not be part of search) primary use for Mutation/Crossover Points
     * @return all READ data items without a corresponding WRITE
     */
    public static Map<Object, List<DataFlowNode>> findUnsatisfiedDataItems(TruffleLanguageInformation information, Node rootNode, Node breakOffPoint) {
        // stack of parent nodes
        Map<Object, List<DataFlowNode>> unsatisfiedItems = new HashMap<>();
        Map<Object, List<Object>> availableDataItems = new HashMap<>();
        Map<Object, List<Object>> satsifyItem = new HashMap<>();

        // we can't allow parents of the break off point to be used, as a parent may rely on its children
        Collection<Node> excludes = ExtendedNodeUtil.parentHierarchy(breakOffPoint);
        if (breakOffPoint != null) {
            excludes.addAll(ExtendedNodeUtil.flatten(breakOffPoint).collect(Collectors.toList()));
        }

        ExtendedNodeUtil.flatten(rootNode).forEach(current -> {
            // current class
            TruffleClassInformation info = information.getClass(current.getClass()) != null ? information.getClass(current.getClass()) : null;

            // load writes at current point
            if (info != null && info.hasProperty(TruffleClassProperty.STATE_WRITE) && !excludes.contains(current)) {
                loadDataItem(availableDataItems, current);
            }
            // load read at current point and check if there is a write for it
            if (info != null && info.hasProperty(TruffleClassProperty.STATE_READ) && !info.hasProperty(TruffleClassProperty.STATE_READ_ARGUMENT) && !excludes.contains(current)) {
                loadDataItem(satsifyItem, current);
                // check writes
                satsifyItem.forEach((key, value) -> {
                    if (!availableDataItems.containsKey(key) || !availableDataItems.get(key).containsAll(value)) {
                        if (!unsatisfiedItems.containsKey(key)) {
                            unsatisfiedItems.put(key, value.stream().map(x -> new DataFlowNode(x, current)).collect(Collectors.toList()));
                        } else {
                            unsatisfiedItems.get(key).addAll(value.stream().map(x -> new DataFlowNode(x, current)).collect(Collectors.toList()));
                        }
                    }
                });
                // clear checked read
                satsifyItem.clear();
            }
        });

        return unsatisfiedItems;
    }

    /**
     * Returns a map of all data items via the given Class Property in the root node until the breakoffPoint (exclusive!)
     * Please note, if you want to mine whatever is IN the breakoff point just apply this function with the breakoffPoint as root node!
     *
     * @param information   language that will be used to find writes
     * @param rootNode      node to be mined
     * @param breakOffPoint subtree NOT searched in tree (if null, all reads will be searched), exclusive (point and its parents will not be part of search) primary use for Mutation/Crossover Points
     * @return all READ data items
     */
    public static Map<Object, List<DataFlowNode>> findDataItems(TruffleLanguageInformation information, Node rootNode, Node breakOffPoint, TruffleClassProperty property) {
        // stack of parent nodes
        Map<Object, List<DataFlowNode>> availableDataItems = new HashMap<>();
        Stack<Iterator<Node>> stack = new Stack<>();
        Iterator<Node> iterator = Collections.emptyIterator();
        Node current = rootNode;

        // we can't allow parents of the break off point to be used, as a parent may rely on its children
        Collection<Node> excludes = ExtendedNodeUtil.parentHierarchy(breakOffPoint);

        while (current != breakOffPoint && current != null) {
            // add available item if there is a write (only for known classes)
            if (information.getClass(current.getClass()) != null && information.getClass(current.getClass()).hasProperty(property) && !excludes.contains(current)) {
                Frame frame = null;
                FrameSlot slot = null;

                // load frame
                List<Field> matFrame = Arrays.stream(current.getClass().getDeclaredFields()).filter(field -> Frame.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
                if (matFrame.size() >= 1) {
                    frame = (Frame) JavaAssistUtil.safeFieldAccess(matFrame.get(0), current);
                    if (matFrame.size() > 1) {
                        logger.warn("Class " + current.getClass() + " has multiple frame fields");
                    }
                }

                // load frame slot
                List<Field> frameSlot = Arrays.stream(current.getClass().getDeclaredFields()).filter(field -> FrameSlot.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
                if (frameSlot.size() >= 1) {
                    slot = (FrameSlot) JavaAssistUtil.safeFieldAccess(frameSlot.get(0), current);
                    if (matFrame.size() > 1) {
                        logger.warn("Class " + current.getClass() + " has multiple frame slot fields");
                    }
                }

                if (!availableDataItems.containsKey(frame)) {
                    availableDataItems.put(frame, new LinkedList<>());
                }
                availableDataItems.get(frame).add(new DataFlowNode(slot, current));

                // load slot
                int i = 1;
            }

            // move to next pos
            if (current.getChildren().iterator().hasNext()) {
                // descend if needed
                stack.push(iterator);
                iterator = current.getChildren().iterator();
                current = iterator.next();
            } else {
                // ascend if needed
                while (!iterator.hasNext() && !stack.isEmpty()) {
                    iterator = stack.pop();
                }

                // move next
                if (iterator.hasNext()) {
                    current = iterator.next();
                } else {
                    // no more items = we are done
                    return availableDataItems;
                }
            }
        }

        return availableDataItems;
    }

    private static void loadDataItem(Map<Object, List<Object>> availableDataItems, Node node) {
        Frame frame = null;
        FrameSlot slot = null;

        // load frame
        List<Field> matFrame = Arrays.stream(node.getClass().getDeclaredFields()).filter(field -> Frame.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
        if (matFrame.size() >= 1) {
            frame = (Frame) JavaAssistUtil.safeFieldAccess(matFrame.get(0), node);
            if (matFrame.size() > 1) {
                logger.warn("Class " + node.getClass() + " has multiple frame fields");
            }
        }

        // load frame slot
        List<Field> frameSlot = Arrays.stream(node.getClass().getDeclaredFields()).filter(field -> FrameSlot.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
        if (frameSlot.size() >= 1) {
            slot = (FrameSlot) JavaAssistUtil.safeFieldAccess(frameSlot.get(0), node);
            if (matFrame.size() > 1) {
                logger.warn("Class " + node.getClass() + " has multiple frame slot fields");
            }
        }

        if (!availableDataItems.containsKey(frame)) {
            availableDataItems.put(frame, new LinkedList<>());
        }
        availableDataItems.get(frame).add(slot);
    }

    /**
     * Returns the data flow graph of all data items in the given tree except the breakoffPoint (exclusive!)
     *
     * @param information   language that will be used to find writes
     * @param rootNode      rootNode of tree to be mined
     * @param breakOffPoint subtree NOT searched in tree, primary use for Mutation/Crossover Points
     * @return data flow graph
     */
    private static DataFlowGraph constructDataFlowGraph(TruffleLanguageInformation information, Node rootNode, Node breakOffPoint) {
        return constructDataFlowGraph(information, rootNode, breakOffPoint, null);
    }

    /**
     * Returns the data flow graph of all data items in the given tree except the breakoffPoint (exclusive!)
     *
     * @param information   language that will be used to find writes
     * @param rootNode      rootNode of tree to be mined
     * @param breakOffPoint subtree NOT searched in tree, primary use for Mutation/Crossover Points
     * @param signature     signature of the function to be included for parameter access
     * @return data flow graph
     */
    public static DataFlowGraph constructDataFlowGraph(TruffleLanguageInformation information, Node rootNode, Node breakOffPoint, TruffleFunctionSignature signature) {
        return new DataFlowGraph(rootNode, findAvailableDataItems(information, rootNode, breakOffPoint),
                findUnsatisfiedDataItems(information, rootNode, breakOffPoint), signature);
    }
}
