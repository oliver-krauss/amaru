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


import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleParameterInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for most sensible strategies. Selects the current and next value from a list of known values.
 * Note that next is intentionally not implemented as this is part of the specific strategy.
 *
 * @param <T> Type of value
 */
public class KnownValueStrategy<T> extends DefaultObservableStrategy implements TruffleSimpleStrategy<T> {

    protected int max_retries = 100;

    /**
     * List of values that can be used by an implementing strategy
     */
    protected Collection<T> values;

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<T> chooser = new RandomChooser<>();

    /**
     * Constructor to enforce that values is never null
     */
    public KnownValueStrategy() {
        this.values = new LinkedList<>();
    }

    /**
     * One can set the list of values that are allowed to be used by the implementing strategies
     *
     * @param values to be used
     */
    public KnownValueStrategy(Collection<T> values) {
        this.values = values;
    }

    /**
     * Value to be added to the strategy. Does not check for duplicates!
     *
     * @param value to be added
     */
    public void addValue(T value) {
        values.add(value);
        notifyExtension();
        notifyEnable();
    }

    /**
     * removes a value from the strategy, if the value is not part of the strategy nothing happens.
     *
     * @param value to be removed
     */
    public void removeValue(T value) {
        values.remove(value);
        notifyRestriction();
        if (values.isEmpty()) {
            notifyDisable();
        }
    }

    /**
     * returns a copy of the values in this strategy.
     */
    public Collection<T> getValues() {
        return new LinkedList<>(values);
    }

    /**
     * Currently created object
     */
    protected T current;

    @Override
    public T current() {
        if (current == null) {
            return next();
        }
        return current;
    }

    @Override
    public T next() {
        return chooser.choose(values);
    }

    /**
     * We enforce a cloneable as we want to be able to copy this strategy
     *
     * @return
     */
    @Override
    public KnownValueStrategy<T> clone() {
        return new KnownValueStrategy<>(new LinkedList<>(values));
    }

    public void setChooser(ChooseOption<T> chooser) {
        this.chooser = chooser;
    }

    @Override
    public Collection<Class> getManagedClasses() {
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(this.current().getClass());
        return classes;
    }

    @Override
    public T create(CreationInformation information) {
        Collection<Requirement> requirements = information != null ? information.getRequirements().getRequirements(Requirement.REQ_VALUE_RESTRICTED) : null;
        if (requirements != null && !requirements.isEmpty()) {
            List<Object> collect = requirements.stream().map(x -> x.getProperty(Requirement.REQ_VALUE_VALUE, Object.class)).collect(Collectors.toList());
            requirements.forEach(x -> information.getRequirements().fullfill(x));
            T next = next();
            int tries = 0;
            while (collect.contains(next) && max_retries > tries++) {
                next = next();
            }
            if (tries >= max_retries) {
                throw new RuntimeException("Created values are not allowed due to restrictions");
            }
            return next;
        }
        return next();
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        // set all value reqs to true. As the patterns are right now we KNOW we can fulfill anyway.
        information.getRequirements().getRequirements().entrySet().stream().filter(x -> x.getKey().getName().equals(Requirement.REQ_VALUE_RESTRICTED)).forEach(x -> x.setValue(1));
        return values != null && !values.isEmpty() ? information.getRequirements() : null;
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation information, Map<Node, LoadedRequirementInformation> requirementMap) {
        Requirement tciPlaceholder = requirementMap.get(ast).getRequirements().stream().filter(x -> x.getName().equals("TCI_PLACEHOLDER")).findFirst().get();
        TruffleClassInformation tci = tciPlaceholder.getProperty("TCI", TruffleClassInformation.class);
        TruffleParameterInformation pInfo = tciPlaceholder.getProperty("PINFO", TruffleParameterInformation.class);

        information.getRequirements().keySet().removeIf(req -> {
            if (!req.getName().equals(Requirement.REQ_VALUE_RESTRICTED) || req.getProperty(Requirement.REQ_VALUE_TYPE, String.class).equals("com.oracle.truffle.api.frame.FrameSlot")) {
                return false;
            }
            Object property = req.getProperty(Requirement.REQ_VALUE_VALUE, Object.class);
            Object loaded = JavaAssistUtil.safeFieldAccess(pInfo.getName(), ast);

            if (property.equals(loaded)) {
                return false;
            }
            return true;
        });

        return requirementMap;
    }
}
