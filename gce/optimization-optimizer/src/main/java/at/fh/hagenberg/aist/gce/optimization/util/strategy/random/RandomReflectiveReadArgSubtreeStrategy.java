/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.random;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleFunctionSignature;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.RandomArraySizeStrategy;
import com.oracle.truffle.api.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RandomReflectiveReadArgSubtreeStrategy<T extends Node> extends RandomReflectiveSubtreeStrategy<T> {

    public RandomReflectiveReadArgSubtreeStrategy(TruffleClassInformation information, List<Class> classes, Map<String, TruffleVerifyingStrategy> terminalStrategies, TruffleHierarchicalStrategy nonTerminalStrategy) {
        super(information, classes, terminalStrategies, nonTerminalStrategy);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        if (information.getDataFlowGraph() != null && information.getDataFlowGraph().getSignature() != null) {
            // only allow this class to be used if a parameter that it understands exists
            return Arrays.stream(information.getDataFlowGraph().getSignature().getArguments()).anyMatch(x -> this.information.getArgumentReadClasses().contains(x)) ? super.canCreate(information) : null;
        }
        return super.canCreate(information);
    }

    /**
     * Restricts this strategy to only create reads on arguments that are of a readable type
     *
     * @param signature to be restricted towards
     * @return if the strategy can still produce valid nodes
     */
    public boolean restrictBySignature(TruffleFunctionSignature signature) {
        if (Arrays.stream(signature.getArguments()).noneMatch(x -> this.information.getArgumentReadClasses().contains(x))) {
            this.disabled = true;
            notifyDisable();
            return false;
        }

        if (this.terminalStrategies.size() == 0) {
            // strategies without a terminal are managing arguments in a different way from what we are looking for here
            return true;
        }

        if (!this.terminalStrategies.containsKey("int") || this.terminalStrategies.size() > 1 || this.initializer.getParameters().length != 1) {
            throw new RuntimeException("ERROR: Currently the Argument Read strategies assume there is only one index parameter. When this error occurs we must extend this code as it is now insufficient");
        }

        // load valid slots and add them to the strategy
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < signature.size(); i++) {
            if (this.information.getArgumentReadClasses().contains(signature.getArguments()[i])) {
                positions.add(i);
            }
        }
        this.terminalStrategies.put("int", new KnownValueStrategy<Integer>(positions));
        return true;
    }
}
