/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.other;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * Special Strategy for the Truffle Node Sequence Creator.
 * Should not be used in any other context.
 */
public class StaticObjectStrategy<T> implements TruffleSimpleStrategy<T> {

    private T object;

    public StaticObjectStrategy(T object) {
        this.object = object;
    }

    @Override
    public T current() {
        return object;
    }

    @Override
    public T next() {
        return object;
    }

    @Override
    public T create(CreationInformation information) {
        return current();
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return object != null ? information.getRequirements() : null;
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        return requirementMap;
    }
}
