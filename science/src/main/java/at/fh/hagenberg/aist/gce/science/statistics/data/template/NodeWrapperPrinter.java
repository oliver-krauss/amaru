/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.template;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Printer for the Node Wrapper class based on freemarker
 *
 * @author Oliver Krauss on 23.10.2019
 */
public class NodeWrapperPrinter extends FreemarkerPrinter {

    /**
     * Identifier of simple template
     */
    private static final String NODE_WRAPPER_TEMPLATE = "nodeWrapper";

    /**
     * Identifier of template for printing multiple ASTs at the same time
     */
    private static final String NODE_WRAPPER_GROUP_TEMPLATE = "nodeWrapperGroup";

    /**
     * Debug flag, adding additional information to the prints
     */
    private boolean debug = false;

    /**
     * TLI for more exhaustive parsing of truffle language
     */
    private TruffleLanguageInformation tli;

    public NodeWrapperPrinter(String templateDirectory) {
        this(templateDirectory, false);
    }

    public NodeWrapperPrinter(String templateDirectory, boolean debug) {
        super(templateDirectory);
        this.setSupportedFormats(Arrays.asList("html"));
        this.addTemplate(NODE_WRAPPER_TEMPLATE, NODE_WRAPPER_TEMPLATE);
        this.addTemplate(NODE_WRAPPER_GROUP_TEMPLATE, NODE_WRAPPER_GROUP_TEMPLATE);
        this.setDebug(debug);
    }

    public void printNodeWrapper(NodeWrapper node) {
        node = prepareNode(node);
        if (!debug) {
            makeReadable(node);
        }
        super.transform(NODE_WRAPPER_TEMPLATE, node);
    }

    public void printNodeWrapperGroup(Collection<NodeWrapper> nodes) {
        nodes = nodes.stream().map(this::prepareNode).collect(Collectors.toList());
        if (!debug) {
            nodes.forEach(this::makeReadable);
        }
        super.transform(NODE_WRAPPER_GROUP_TEMPLATE, nodes);
    }


    /**
     * Modify nodes for printing
     *
     * @param node
     */
    private NodeWrapper prepareNode(NodeWrapper node) {
        node = node.deepCopy();
        // Nodes MUST have Ids. If they don't we have to create them
        if (node.getId() == null) {
            tempId = 0;
            tempId(node);
        }
        return node;
    }

    /**
     * Temporary Id counter
     */
    long tempId = 0;

    /**
     * Helper to write temporary ids
     * @param node with temporary ids
     */
    private void tempId(NodeWrapper node) {
        node.setId(tempId++);
        node.getChildren().forEach(x -> tempId(x.getChild()));
    }

    /**
     * Helper function that reduces the names of nodes to the simpleName
     * @param node to be made readable
     */
    private void makeReadable(NodeWrapper node) {
        // if TLI is available modify the graph to look better
        if (tli != null) {
            TruffleClassInformation tci = tli.getClass(node.getType());
            if (tci != null && tci.getShortName() != null) {
                node.setType(tci.getShortName());
                node.getChildren().forEach(x -> makeReadable(x.getChild()));
            }
        }
    }

    public String getNodeWrapperTemplate() {
        return this.getTemplateMap().get(NODE_WRAPPER_TEMPLATE);
    }

    public void setNodeWrapperTemplate(String template) {
        this.addTemplate(NODE_WRAPPER_TEMPLATE, template);
    }

    public TruffleLanguageInformation getTli() {
        return tli;
    }

    public void setTli(TruffleLanguageInformation tli) {
        this.tli = tli;
    }


    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.addAdditionalTemplateValue("debug", debug);
    }
}
