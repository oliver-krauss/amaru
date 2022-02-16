/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.parser;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that contains the description of the struct
 * Created by Oliver Krauss on 28.09.2017.
 * TODO #11 finish
 */
@NodeInfo(shortName = "struct", description = "C Struct representation")
public class MinicStructNode extends MinicNode {

    /**
     * frame descriptor that contains the struct-values
     */
    FrameDescriptor descriptor;

    /**
     * Scope the struct is in
     */
    MinicNodeFactory.LexicalScope scope;

    /**
     * These are all nodes that MUST be called for initialization of the frame
     */
    private List<MinicNode> initializationNodes = new ArrayList<>();

    public MinicStructNode() {

    }

    public MinicStructNode(MinicNodeFactory.LexicalScope scope, FrameDescriptor descriptor) {
        this.scope = scope;
        this.descriptor = descriptor;
    }

    public FrameDescriptor getDescriptor() {
        return descriptor;
    }

    public MinicNodeFactory.LexicalScope getScope() {
        return scope;
    }

    public List<MinicNode> getInitializationNodes() {
        return initializationNodes;
    }

    public void setInitializationNodes(List<MinicNode> initializationNodes) {
        this.initializationNodes = initializationNodes;
    }

    public void addInitializationNode(MinicNode initializationNode) {
        this.initializationNodes.add(initializationNode);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // do nothing struct nodes can't be called
    }
}
