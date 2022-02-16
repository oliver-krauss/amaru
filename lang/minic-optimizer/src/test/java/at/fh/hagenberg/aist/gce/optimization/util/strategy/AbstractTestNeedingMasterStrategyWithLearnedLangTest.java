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

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.util.MinicLanguageLearner;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.KnownValueStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleMasterStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.*;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.frame.*;
import org.graalvm.polyglot.Context;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AbstractTestNeedingMasterStrategyWithLearnedLangTest {

    @BeforeClass
    public static void setUp() {
        Context context = Context.newBuilder().out(System.out).build();
        context.initialize(MinicLanguage.ID);
    }


    protected TruffleMasterStrategy strategy;

    protected FrameDescriptor frameDescriptor;

    protected Frame frame;

    protected TruffleMasterStrategy create() {
        if (strategy != null) {
            return strategy;
        }
        CreationConfiguration configuration = new CreationConfiguration(10, 10, Double.MAX_VALUE);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // LEARN LANGUAGE to get necessary meta info
        MinicLanguageLearner minicLanguageLearner = new MinicLanguageLearner(information);
        minicLanguageLearner.setFast(true);
        minicLanguageLearner.setSaveToDB(false);
        minicLanguageLearner.learn();

        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        strategies.put("int", new KnownValueStrategy<Integer>(new IntDefault().getValues()));
        strategies.put("char", new KnownValueStrategy<Character>(new CharDefault().getValues()));
        strategies.put("double", new KnownValueStrategy<Double>(new DoubleDefault().getValues()));
        strategies.put("float", new KnownValueStrategy<Float>(new FloatDefault().getValues()));
        strategies.put("java.lang.String", new KnownValueStrategy<String>(new StringDefault().getValues()));
        strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(new MaterializedFrame() {
            @Override
            public FrameDescriptor getFrameDescriptor() {
                FrameDescriptor d = new FrameDescriptor();
                d.addFrameSlot("global");
                return d;
            }

            @Override
            public Object[] getArguments() {
                return new Object[0];
            }

            @Override
            public Object getObject(FrameSlot slot) throws FrameSlotTypeException {
                return null;
            }

            @Override
            public void setObject(FrameSlot slot, Object value) {

            }

            @Override
            public byte getByte(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setByte(FrameSlot slot, byte value) {

            }

            @Override
            public boolean getBoolean(FrameSlot slot) throws FrameSlotTypeException {
                return false;
            }

            @Override
            public void setBoolean(FrameSlot slot, boolean value) {

            }

            @Override
            public int getInt(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setInt(FrameSlot slot, int value) {

            }

            @Override
            public long getLong(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setLong(FrameSlot slot, long value) {

            }

            @Override
            public float getFloat(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setFloat(FrameSlot slot, float value) {

            }

            @Override
            public double getDouble(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setDouble(FrameSlot slot, double value) {

            }

            @Override
            public Object getValue(FrameSlot slot) {
                return null;
            }

            @Override
            public MaterializedFrame materialize() {
                return null;
            }

            @Override
            public boolean isObject(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isByte(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isBoolean(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isInt(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isLong(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isFloat(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isDouble(FrameSlot slot) {
                return false;
            }
        }));
        frameDescriptor = new FrameDescriptor();
        frameDescriptor.addFrameSlot("asdf");
        frame = new VirtualFrame() {
            @Override
            public FrameDescriptor getFrameDescriptor() {
                return null;
            }

            @Override
            public Object[] getArguments() {
                return new Object[0];
            }

            @Override
            public Object getObject(FrameSlot slot) throws FrameSlotTypeException {
                return null;
            }

            @Override
            public void setObject(FrameSlot slot, Object value) {

            }

            @Override
            public byte getByte(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setByte(FrameSlot slot, byte value) {

            }

            @Override
            public boolean getBoolean(FrameSlot slot) throws FrameSlotTypeException {
                return false;
            }

            @Override
            public void setBoolean(FrameSlot slot, boolean value) {

            }

            @Override
            public int getInt(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setInt(FrameSlot slot, int value) {

            }

            @Override
            public long getLong(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setLong(FrameSlot slot, long value) {

            }

            @Override
            public float getFloat(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setFloat(FrameSlot slot, float value) {

            }

            @Override
            public double getDouble(FrameSlot slot) throws FrameSlotTypeException {
                return 0;
            }

            @Override
            public void setDouble(FrameSlot slot, double value) {

            }

            @Override
            public Object getValue(FrameSlot slot) {
                return null;
            }

            @Override
            public MaterializedFrame materialize() {
                return null;
            }

            @Override
            public boolean isObject(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isByte(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isBoolean(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isInt(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isLong(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isFloat(FrameSlot slot) {
                return false;
            }

            @Override
            public boolean isDouble(FrameSlot slot) {
                return false;
            }
        };
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(frameDescriptor, frame));
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.getCurrentContext()));
        TruffleLanguageSearchSpace tss = new TruffleLanguageSearchSpace(information, null);
        return strategy = TruffleMasterStrategy.createFromTLI(configuration, tss, new ArrayList<>(), strategies);
    }

}
