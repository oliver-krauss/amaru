/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.control;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

/**
 * If..Then..Else node
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "if", description = "If condition Then path Else (optional) path")
public class MinicIfNode extends MinicNode {

    /**
     * Condition. If true then path will be taken, otherwise the else path
     */
    @Child
    private MinicExpressionNode condition;

    /**
     * Path if condition is true
     */
    @Child
    private MinicNode thenPath;

    /**
     * Path if condition is false
     */
    @Child
    private MinicNode elsePath;

    private final ConditionProfile conditionProfile = ConditionProfile.createBinaryProfile();

    public MinicIfNode(MinicExpressionNode condition, MinicNode thenPath, MinicNode elsePath) {
        this.condition = condition;
        this.thenPath = thenPath;
        this.elsePath = elsePath;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        if (this.conditionProfile.profile(this.evaluateCondition(frame))) {
            this.thenPath.executeVoid(frame);
        } else {
            if (this.elsePath != null) {
                this.elsePath.executeVoid(frame);
            }
        }
    }

    private boolean evaluateCondition(VirtualFrame frame) {
        return !this.condition.executeGeneric(frame).equals(0);
    }
}
