/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Requirement that must be met in order to create a valid tree.
 * As how we engineer these requirements is still hazy it currently only has a name and properties
 * <p>
 * currently known requirements:
 * - including a specific write for something
 *
 * @author Oliver Krauss on 31.07.2020
 */
public class Requirement {

    private static long ID = 0;

    // reference to another requirement
    public static final String REQ_REF = "requirement-reference";

    // Constants for data write
    public static String REQ_DATA_WRITE = "data-write";
    /**
     * is a {@link com.oracle.truffle.api.frame.FrameSlot}
     */
    public static String REQPR_SLOT = "slot";
    /**
     * is a {@link com.oracle.truffle.api.frame.Frame}
     */
    public static String REQPR_FRAME = "frame";

    /**
     * Antipattern Requirement
     */
    public static String REQ_ANTIPATTERN = "pattern-anti";

    /**
     * Pattern Requirement
     */
    public static String REQ_PATTERN = "pattern";

    /**
     * Pattern Requirement
     */
    public static String REQ_PATTERN_ACTIVATION_CHANCE = "pattern-chance";

    /**
     * Is a {@link at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper} of the entire pattern
     */
    public static String REQPR_PATTERN = "pattern";

    /**
     * Is a {@link at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper} of the current position to be checked
     */
    public static String REQPR_PATTERN_POS = "pattern-pos";

    /**
     * Contains the position (left to right) counter when a node has multiple children.
     */
    public static String REQPR_PATTERN_LTRPOS = "pattern-ltr-pos";

    /**
     * Contains the field that is currently being ltr iterated
     */
    public static final String REQPR_PATTERN_LTRFIELD = "pattern-ltr-field";

    /**
     * The owner of the positions that has to be moved to the next position
     */
    public static final String REQPR_PATTERN_LTROWNER = "pattern-ltr-owner";

    /**
     * Contains the maximum number of the pattern ltr pos when a node has multiple children.
     */
    public static String REQPR_PATTERN_LTRPOS_MAX = "pattern-ltr-pos-max";

    /**
     * Determines how a pattern must be matched (default is only once - anywhere)
     */
    public static final String REQ_PATTERN_MATCH_TYPE = "pattern-match-type";

    /**
     * Maximum width the children of a given node are allowed to have
     */
    public static final String REQPR_MAX_WIDTH = "max-width";

    /**
     * Map for variable placeholders
     * Map<String, Pair<FrameSlot, TruffleClassInformation>> -> where key = placeholder ID; value.key = FrameSlot to be loaded | value.value = Type that the frame slot was first created with
     */
    public static final String REQ_PATTERN_VAR_PLACEHOLDER = "pattern-vars";
    /**
     * Mach must be everywhere in the subtree
     */
    public static final String REQ_PATTERN_MATCH_TYPE_EVERYWHERE = "everywhere";

    /**
     * Forbidden terminal value.
     */
    public static final String REQ_VALUE_RESTRICTED = "value-restriction";

    /**
     * On value requirements this is the data type
     */
    public static final String REQ_VALUE_TYPE = "value-datatype";

    /**
     * On value requirements this is the value as object
     */
    public static final String REQ_VALUE_VALUE = "value-value";


    /**
     * Name of the requirement. Such as "data-write"
     */
    public String name;

    private Integer propertyHash;

    /**
     * Properties of the requirement. May be null
     * Ex. slot -> the frame slot for the "data-write" requirement
     */
    public Map<String, Object> properties = new HashMap<>();

    private long id;

    public Requirement(String name) {
        id = ID++;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public <T> T getProperty(String name, Class<T> type) {
        return type.cast(this.properties.get(name));
    }

    public boolean containsProperty(String name) {
        return this.properties.containsKey(name);
    }

    public Requirement addProperty(String name, Object value) {
        this.properties.put(name, value);
        propertyHash = null;
        return this;
    }

    public Requirement setProperties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Requirement)) return false;
        Requirement that = (Requirement) o;
        return Objects.equals(name, that.name) &&
                getPropertyHash() == that.getPropertyHash();
    }

    @Override
    public int hashCode() {
        return name.hashCode() + getPropertyHash();
    }

    private int getPropertyHash() {
        if (propertyHash == null) {
            propertyHash = properties.hashCode();
        }
        return propertyHash;
    }

    public Requirement copy() {
        HashMap<String, Object> propCopy = new HashMap<>();
        properties.forEach((k, v) -> {
            Object vCopy = v;
            if (v instanceof Requirement) {
                vCopy = ((Requirement) v).copy();
            }
            propCopy.put(k, vCopy);
        });
        return new Requirement(name).setProperties(propCopy);
    }

    /**
     * Searches the req in the given requirements.
     * It does NOT do an "equals", but instead a soft match over the id
     *
     * @param req          to be found
     * @param requirements to be searched in
     * @return requirement or NULL if not contained
     */
    public static Requirement loadMatch(Requirement req, RequirementInformation requirements) {
        return loadMatch(req, requirements.getRequirements(req.getName()));
    }

    /**
     * Searches the req in the given requirements.
     * It does NOT do an "equals", but instead a soft match over the id
     *
     * @param req          to be found
     * @param requirements to be searched in
     * @return requirement or NULL if not contained
     */
    public static Requirement loadMatch(Requirement req, Collection<Requirement> requirements) {
        if (!req.containsProperty("ID")) {
            return null;
        }
        int id = req.getProperty("ID", Integer.class);
        NodeWrapper pattern = req.getProperty(Requirement.REQ_PATTERN, NodeWrapper.class);
        return requirements.stream().filter(parentReq -> (parentReq.getName().equals(req.getName()) || parentReq.getName().equals(REQ_PATTERN)) && parentReq.getProperty("ID", Integer.class).equals(id) && parentReq.getProperty(Requirement.REQ_PATTERN, NodeWrapper.class).equals(pattern)).findAny().orElse(null);
    }

    public static boolean isMatch(Requirement req, Requirement other) {
        if (!req.containsProperty("ID")) {
            return false;
        }
        int id = req.getProperty("ID", Integer.class);
        return other.containsProperty("ID") && other.getProperty("ID", Integer.class).equals(id) && other.getProperty(Requirement.REQ_PATTERN, NodeWrapper.class).equals(req.getProperty(Requirement.REQ_PATTERN, NodeWrapper.class));
    }

    /**
     * On a NodeWrapper that is an Anywhere Wildcard find the currently active child
     *
     * @param x
     * @param wildcard to be selected
     * @return active child node
     */
    public static NodeWrapper findStarchild(Requirement x, NodeWrapper wildcard) {
        try {
            Integer ltr = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
            if (ltr != null && !x.getProperties().containsKey(Requirement.REQPR_PATTERN_LTROWNER)) {
                Integer max = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class);
                if (ltr.equals(max)) {
                    return null;
                }
                // for multi-child LTR consider current active postion (if owner exists the current active pos is the one from the parent!)
                return wildcard.getChildren().stream().filter(c -> ltr.equals(c.getOrder())).findFirst().get().getChild();
            } else {
                // for single value just always take first
                return wildcard.getChildren().stream().findFirst().get().getChild();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a property exists, and removes it.
     *
     * @param property to be checked
     * @return if the property was there
     */
    public boolean containsAndRemoveProperty(String property) {
        boolean contained = this.containsProperty(property);
        this.properties.remove(property);
        return contained;
    }

}
