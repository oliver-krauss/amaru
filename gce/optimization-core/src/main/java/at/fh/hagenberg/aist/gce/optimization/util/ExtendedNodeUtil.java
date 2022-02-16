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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This utility class gives meta information on nodes or subtrees.
 * Created by Oliver Krauss on 29.11.2017.
 */
public class ExtendedNodeUtil {

    public static boolean isAPINode(NodeWrapper n) {
        return n != null && isAPINode(n.getType());
    }

    public static boolean isAPINode(Node n) {
        return n != null && isAPINode(n.getClass());
    }

    public static boolean isAPINode(Class n) {
        return n != null && isAPINode(n.getName());
    }

    public static boolean isAPINode(String n) {
        return n != null && (n.startsWith("org.graalvm.compiler") || n.startsWith("com.oracle.truffle.api"));
    }

    /**
     * gets the maximum depth of the subtree (downstream)
     *
     * @param node to find depth of
     * @return depth
     */
    public static int maxDepth(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("No node given.");
        }

        int depth = 0;
        Iterator<Node> it = node.getChildren().iterator();
        while (it.hasNext()) {
            int newDepth = maxDepth(it.next());
            depth = depth > newDepth ? depth : newDepth;
        }

        return depth + 1;
    }

    public static double avgDepth(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("No node given.");
        }

        double depth = 0;
        double ownWidth = 1;
        Iterator<Node> it = node.getChildren().iterator();
        while (it.hasNext()) {
            double newDepth = avgWidth(it.next());
            depth += newDepth;
            ownWidth++;
        }
        depth = depth / ownWidth;

        return depth;
    }

    public static int maxWidth(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("No node given.");
        }

        int width = 1;
        int ownWidth = 1;
        Iterator<Node> it = node.getChildren().iterator();
        while (it.hasNext()) {
            int newWidth = maxWidth(it.next());
            ownWidth++;
            width = ownWidth > newWidth ? ownWidth : newWidth;
        }

        return width;
    }

    public static double avgWidth(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("No node given.");
        }

        double width = 1;
        double ownWidth = 1;
        Iterator<Node> it = node.getChildren().iterator();
        while (it.hasNext()) {
            double newWidth = avgWidth(it.next());
            ownWidth++;
            width += newWidth + 1;
        }
        width = width / ownWidth;

        return width;
    }

    /**
     * Gets the depth of where the given node is in the tree (upstream)
     *
     * @param node whose depth is to be determined
     * @return depth of node in hierarchy, 0 if it is has no parent
     */
    public static int getDepth(Node node) {
        int depth = 0;
        while (node != null) {
            depth++;
            node = node.getParent();
        }
        return depth;
    }

    /**
     * @param parentNode a parent node used as depth = 0
     * @param childNode  a node somewhere below the parent node in the tree
     * @return the depth, or -1 if the child node is not actually a child of the parent node, or 0 if the nodes are one and the same
     */
    public static int getRelativeDepth(Node parentNode, Node childNode) {
        int depth = 0;
        while (childNode != parentNode && childNode != null) {
            depth++;
            childNode = childNode.getParent();
        }
        return childNode != null ? depth : -1;
    }

    /**
     * Calculates the amout of nodes in a tree
     *
     * @param tree to be evaluated
     * @return amount of nodes in tree (inclusive!)
     */
    public static int size(Node tree) {
        return 1 + StreamSupport.stream(tree.getChildren().spliterator(), true).mapToInt(ExtendedNodeUtil::size).sum();
    }

    /**
     * Turns a Tree into a flat stream (as stream to enable subsequent filtering etc.)
     *
     * @param nodes to be turned into a flat stream
     * @return stream of all nodes intree
     */
    public static Stream<Node> flatten(Node nodes) {
        return flatten(Stream.of(nodes));
    }

    /**
     * Turns a Tree into a flat stream (as stream to enable subsequent filtering etc.)
     *
     * @param nodes to be turned into a flat stream
     * @return stream of all nodes intree
     */
    public static Stream<Node> flatten(Stream<Node> nodes) {
        return nodes.flatMap(x -> Stream.concat(Stream.of(x), flatten(StreamSupport.stream(x.getChildren().spliterator(), true))));
    }

    /**
     * Provides all parent relationships up from the given node
     *
     * @param node to be checked for parents
     * @return all parent relationships to null
     */
    public static Collection<Node> parentHierarchy(Node node) {
        List<Node> parents = new LinkedList<>();
        if (node != null) {
            Node parent = node.getParent();
            while (parent != null) {
                parents.add(parent);
                parent = parent.getParent();
            }
        }
        return parents;
    }

    /**
     * Provides all nodes that this node may affect (eg. parents + right siblings)
     *
     * @param node to be checked
     * @return all nodes up and right
     */
    public static Collection<Node> rightContext(Node node) {
        Collection<Node> nodes = new ArrayList<>();
        Node parent = node.getParent();
        Node current = node;

        while (parent != null) {
            nodes.add(parent);
            Iterator<Node> iterator = parent.getChildren().iterator();
            boolean match = false;
            while (iterator.hasNext()) {
                Node next = iterator.next();
                if (match) {
                    // add sibling node and all its children
                    nodes.addAll(flatten(next).collect(Collectors.toList()));
                } else if (next == current) {
                    match = true;
                }
            }
            match = false;
            current = parent;
            parent = current.getParent();

        }

        return nodes;
    }

    /**
     * Provides all nodes that this node may affect (eg. left siblings)
     *
     * @param node to be checked
     * @return all nodes up and right
     */
    public static Collection<Node> leftContext(Node node) {
        Collection<Node> nodes = new ArrayList<>();
        Node parent = node.getParent();
        Node current = node;

        while (parent != null) {
            Iterator<Node> iterator = parent.getChildren().iterator();
            boolean match = false;
            while (iterator.hasNext() && !match) {
                Node next = iterator.next();
                if (next == current) {
                    match = true;
                } else {
                    nodes.addAll(flatten(next).collect(Collectors.toList()));
                }
            }
            current = parent;
            parent = current.getParent();
        }

        return nodes;
    }
}
