/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.sequential;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleParameterInformation;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Creates the truffle tree in a sequence
 */
public class TruffleNodeSequenceCreator extends TruffleSequentialStrategy<Object> {

    /**
     * All classes that shall be initialized by this sequence creator
     */
    private Collection<Class> options;

    /**
     * Iterator over all classes that this sequence will initialize (with all possible-children!)
     */
    private Iterator<Class> sequence;

    /**
     * Currently used class of the sequence iterator;
     */
    private TruffleClassInformation initializer;

    /**
     * Currently created object
     */
    private Object current;

    /**
     * Information about the nodes to be created
     */
    private final TruffleLanguageSearchSpace searchSpace;

    /**
     * parameters of the "current" initializer that need to be created
     */
    private List<TruffleSequentialStrategy> parameters[];

    private final int height;

    private int currentWidth = 1;

    private final int width;

    FrameDescriptor localFrame;
    MaterializedFrame globalFrame;
    Object context;
    String contextClass;

    /**
     * Maximum width any layer of the hiearchy may have
     */
    private final int maxWidth;

    public TruffleNodeSequenceCreator(TruffleLanguageSearchSpace searchSpace, Collection<Class> options, int height, int width, int maxWidth, FrameDescriptor localFrame, MaterializedFrame globalFrame, Object context, String contextClass) {
        super(new LinkedList<>()); // we override EVERYTHING
        this.options = options;
        this.searchSpace = searchSpace;
        this.sequence = options.iterator();
        this.height = height;
        this.width = width;
        this.maxWidth = maxWidth;
        this.localFrame = localFrame;
        this.globalFrame = globalFrame;
        this.context = context;
        this.contextClass = contextClass;
        nextInSequence();
    }

    private void nextInSequence() {
        initializer = searchSpace.getInstantiableNodes().get(sequence.next());
        parameters = new List[currentWidth];
        for (int i = 0; i < currentWidth; i++) {
            parameters[i] = new ArrayList<>();
            for (TruffleParameterInformation parameter : initializer.getInitializersForCreation().get(0).getParameters()) {
                switch (parameter.getType().getName()) {
                    case "int":
                        parameters[i].add(new TruffleSequentialStrategy<Integer>(new IntDefault().getValues()));
                        break;
                    case "char":
                        parameters[i].add(new TruffleSequentialStrategy<Character>(new CharDefault().getValues()));
                        break;
                    case "double":
                        parameters[i].add(new TruffleSequentialStrategy<Double>(new DoubleDefault().getValues()));
                        break;
                    case "float":
                        parameters[i].add(new TruffleSequentialStrategy<Float>(new FloatDefault().getValues()));
                        break;
                    case "java.lang.String":
                        parameters[i].add(new TruffleSequentialStrategy<String>(new StringDefault().getValues()));
                        break;
                    case "com.oracle.truffle.api.frame.MaterializedFrame":
                        parameters[i].add(new StaticSequentialObjectStrategy(globalFrame));
                        break;
                    case "com.oracle.truffle.api.frame.FrameSlot":
                        parameters[i].add(new FrameSlotSequentialStrategy(localFrame));
                        break;
                    default: {
                        if (parameter.getType().getName().equals(contextClass)) {
                            parameters[i].add(new StaticSequentialObjectStrategy(context));
                            break;
                        }
                        Class type = parameter.getType().isArray() ? parameter.getType().getComponentType() : parameter.getType();
                        parameters[i].add(new TruffleNodeSequenceCreator(searchSpace,
                            height <= 1 ? searchSpace.getOperands().get(type) : searchSpace.getOperators().get(type),
                            height - 1,
                            parameter.getType().isArray() ? maxWidth : 1,
                            maxWidth, localFrame, globalFrame, context, contextClass));
                    }
                }
            }
        }
    }

    @Override
    public void resetSequence() {
        // reset array-width
        currentWidth = 1;

        // reset sequence
        this.sequence = options.iterator();
        nextInSequence();
        current = null;
    }

    @Override
    public Object current() {
        if (current == null) {
            return next();
        }
        return current;
    }

    @Override
    public Object next() {
        // find next valid solution or jump back to start of sequence
        boolean notFound = true;
        int i = 0;
        while (notFound && i < currentWidth) {
            Iterator<TruffleSequentialStrategy> seqIt = parameters[i].iterator();
            List<TruffleSequentialStrategy> reset = new ArrayList<>();

            while (notFound && seqIt.hasNext()) {
                TruffleSequentialStrategy sC = seqIt.next();
                if (sC.hasNext()) {
                    // select the first creator that still has a valid option, and reset ALL beforehand
                    // Logic -> hasNext=false moves from left to right, after all options exhausted -> all hasNext = false
                    notFound = false;
                    sC.next();

                    // reset all previous values
                    reset.forEach(x -> x.resetSequence());
                    for (int j = 0; j < i; j++) {
                        parameters[j].forEach(x -> x.resetSequence());
                    }
                } else {
                    reset.add(sC);
                }
            }
            i++;
        }

        // If we have no valid subtree we must move on to the next class or array-width
        if (notFound) {
            // TODO: #28 Arrays (width-counting) not supported yet
            if (!sequence.hasNext()) {
                // if we have no next class we start at the beginning
                sequence = options.iterator();
            }
            nextInSequence();
        }

        if (width > 1) {
            current = Arrays.stream(parameters).map(parametersX ->
                initializer.getInitializersForCreation().get(0).instantiate(parametersX.stream().map(x -> x.current()).toArray())
            ).toArray();
        } else {
            current = initializer.getInitializersForCreation().get(0).instantiate(parameters[0].stream().map(x -> x.current()).toArray());
        }
        return current;
    }

    @Override
    public boolean hasNext() {
        return sequence.hasNext() || Arrays.stream(parameters).anyMatch(x -> x.stream().anyMatch(y -> y.hasNext()));
    }
}
