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

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.GraphPrintVisitor;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Oliver Krauss on 30.06.2017.
 */
public class IGVPrinter {

    /**
     * IGV Printer Map
     */
    private static HashMap<String, IGVPrinter> printerMap = new HashMap<>();

    public static IGVPrinter getPrinter(String groupName, String executionIdentifier) {
        String key = groupName + executionIdentifier;
        if (!printerMap.containsKey(key)) {
            printerMap.put(key, new IGVPrinter(groupName, executionIdentifier));
        }
        return printerMap.get(key);
    }

    private IGVPrinter() {
    }

    ;

    private IGVPrinter(String groupName, String executionIdentifier) {
        this.groupName = groupName;
        this.executionIdentifier = executionIdentifier;
    }

    /**
     * Name of group that IGV will print to
     */
    private String groupName;

    /**
     * Identifier of execution that IGV will print to
     */
    private String executionIdentifier;

    public void printToIGV(String name, RootCallTarget target) {
        if (target != null) {
            GraphPrintVisitor graphPrinter = new GraphPrintVisitor();
            graphPrinter.beginGraph(name).visit(target.getRootNode());
        }
    }

    public void printToIGV(Map<String, RootCallTarget> callTargets) {
        GraphPrintVisitor graphPrinter = new GraphPrintVisitor();
        graphPrinter.beginGroup((executionIdentifier != null ? executionIdentifier : "") + " " + groupName);
        for (String callTarget : callTargets.keySet()) {
            if (callTarget != null) {
                graphPrinter.beginGraph(callTarget).visit(callTargets.get(callTarget).getRootNode());
            }
        }
        graphPrinter.printToNetwork(true);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getExecutionIdentifier() {
        return executionIdentifier;
    }

    public void setExecutionIdentifier(String executionIdentifier) {
        this.executionIdentifier = executionIdentifier;
    }
}
