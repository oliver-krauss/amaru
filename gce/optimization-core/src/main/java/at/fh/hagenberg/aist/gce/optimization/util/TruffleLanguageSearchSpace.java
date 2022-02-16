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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Subset of a Truffle Language that contains the nodes to be used for optimization.
 * Created by Oliver Krauss on 23.02.2017.
 */
public class TruffleLanguageSearchSpace implements TruffleLanguageContextProvider {

    /**
     * Information this search space is based on
     */
    private TruffleLanguageInformation information;

    /**
     * Node classes that are part of the search space
     */
    private Map<Class, TruffleClassInformation> instantiableNodeClasses = new HashMap<>();

    /**
     * Map of all classes that are currently unreachable. The map contains the class and the reason it is unreachable
     * Is the Diff between Node-Classes and InstantiableNodeclasses, as well as classes not currently reachable by the strategies
     * the collection is also meant for developers, to see where work still needs to be done
     */
    private Map<Class, String> unreachableClasses = new HashMap<>();

    /**
     * This map contains a list of parent classes for which there are specific instantiations.
     * Key = (Abstract) Class being searched for
     * Value = List of classes that extend this class
     */
    private Map<Class, List<Class>> instantiableClasses = new HashMap<>();

    /**
     * This map contains a list of terminal classes for which there are specific instantiations.
     * Key = (Abstract) Class being searched for
     * Value = List of classes that extend this class
     */
    private Map<Class, List<Class>> instantiableTerminalClasses = new HashMap<>();

    public TruffleLanguageSearchSpace(TruffleLanguageInformation information, Collection<Class> excludes) {
        this.information = information;
        information.getOperators().forEach((key, value) -> {
            this.instantiableClasses.put(key, new LinkedList<>(value));
        });
        information.getOperands().forEach((key, value) -> {
            this.instantiableTerminalClasses.put(key, new LinkedList<>(value));
        });
        this.unreachableClasses.putAll(information.getUnreachableClasses());

        if (excludes != null && !excludes.isEmpty()) {
            // purge excludes
            excludes.forEach(x -> {
                instantiableNodeClasses.remove(x);
                instantiableClasses.remove(x);
                instantiableClasses.values().forEach(y -> y.remove(x));
                instantiableTerminalClasses.remove(x);
                instantiableTerminalClasses.values().forEach(y -> y.remove(x));
                unreachableClasses.put(x, "Class was excluded from execution");
            });
        }

        information.getInstantiableNodes().forEach((x, info) -> {
            instantiableNodeClasses.put(x, info.copy(this));
        });

        // ensure that we initialize the minimal depth of our class information
        int uninitializedPreviously = 0;
        AtomicInteger uninitialized = new AtomicInteger(instantiableNodeClasses.size());
        while (uninitialized.get() != 0 && uninitialized.get() != uninitializedPreviously) {
            uninitializedPreviously = uninitialized.get();
            uninitialized.set(0);
            instantiableNodeClasses.values().forEach(x -> {
                int i = x.getMinimalSubtreeSize();
                if (i < 0) {
                    uninitialized.getAndIncrement();
                }
            });
        }

        // remove all classes that can't determine their minimal depth, as they are missing related nodes and can't be instantiated
        new HashSet<>(instantiableNodeClasses.entrySet()).stream().filter(x -> x.getValue().getMinimalSubtreeSize() < 0).forEach(x -> {
            unreachableClasses.put(x.getKey(), "Class excluded, as necessary child classes were excluded as well");
            instantiableNodeClasses.remove(x.getKey());
        });

        // we have to updated the write pairings to remove dead dependencies and resolve the context specific classes
        this.getInstantiableNodes().values().forEach(x -> {
            List<TruffleClassInformation> oldWritePairings = new ArrayList<>(x.getWritePairings());
            x.getWritePairings().clear();
            oldWritePairings.forEach(y -> {
                if (this.getInstantiableNodes().containsKey(y.getClazz())) {
                    x.getWritePairings().add(this.getInstantiableNodes().get(y.getClazz()));
                }
            });
        });
    }

    @Override
    public Map<Class, TruffleClassInformation> getInstantiableNodes() {
        return instantiableNodeClasses;
    }

    @Override
    public Map<Class, List<Class>> getOperators() {
        return instantiableClasses;
    }

    @Override
    public Map<Class, List<Class>> getOperands() {
        return instantiableTerminalClasses;
    }

    public Map<Class, String> getUnreachableClasses() {
        return unreachableClasses;
    }

    /**
     * Notifies the TSS that a given class is not manageable in the optimization context
     *
     * @param clazz  that can't be reached
     * @param reason human readable reason for why the class can't be reached
     */
    public void addUnreachableClass(Class clazz, String reason) {
        unreachableClasses.put(clazz, reason);
    }

    public TruffleLanguageInformation getInformation() {
        return information;
    }
}
