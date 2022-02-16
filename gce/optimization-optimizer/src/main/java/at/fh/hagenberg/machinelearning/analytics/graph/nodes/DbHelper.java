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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.types.Node;

/**
 * Helper class that casts an DB Node into a NodeWrapper class
 *
 * @author Oliver Krauss on 12.12.2018
 */

public class DbHelper {

    /**
     * Transforms db node to NodeWrapper class it does NOT set the relationships between them
     *
     * @param n to be transformed
     * @return NodeWrapper with values
     */
    public static NodeWrapper cast(Node n) {
        NodeWrapper node = new NodeWrapper(n.get("type").asString());
        node.setId(n.id());
        n.keys().forEach(key -> {
            if (key.startsWith("values.")) {
                Object value = n.get(key);
                String type = key.substring(key.indexOf(":") + 1);
                node.getValues().put(key.substring(key.indexOf(".") + 1), type.equals("int") ? ((Long) ((Value) value).asObject()).intValue() : (value instanceof StringValue ? ((Value) value).asString() : value));
            }
        });
        return node;
    }


}
