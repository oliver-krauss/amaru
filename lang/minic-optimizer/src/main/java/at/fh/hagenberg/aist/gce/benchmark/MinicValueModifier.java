/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.benchmark;

import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.optimization.executor.ValueModifier;
import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;

public class MinicValueModifier extends ValueModifier {

    @Override
    public Object[] toLanguage(Object[] input) {
        if (input == null) {
            return null;
        }

        Object[] newInputs = new Object[input.length];

        for (int i = 0; i < input.length; i++) {
            Object in = input[i];

            if (in instanceof char[]) {
                // if calling with int[] from java mod this on the first call so users of the languate don't need to cast themselves
                char[] rawArray = (char[]) in;
                MinicCharArray minicArray = new MinicCharArray(new int[]{rawArray.length});
                for (int j = 0; j < rawArray.length; j++) {
                    minicArray.setAtPos(new int[]{j}, rawArray[j]);
                }
                in = minicArray;
            } else if (in instanceof double[]) {
                // if calling with int[] from java mod this on the first call so users of the languate don't need to cast themselves
                double[] rawArray = (double[]) in;
                MinicDoubleArray minicArray = new MinicDoubleArray(new int[]{rawArray.length});
                for (int j = 0; j < rawArray.length; j++) {
                    minicArray.setAtPos(new int[]{j}, rawArray[j]);
                }
                in = minicArray;
            } else if (in instanceof float[]) {
                // if calling with int[] from java mod this on the first call so users of the languate don't need to cast themselves
                float[] rawArray = (float[]) in;
                MinicFloatArray minicArray = new MinicFloatArray(new int[]{rawArray.length});
                for (int j = 0; j < rawArray.length; j++) {
                    minicArray.setAtPos(new int[]{j}, rawArray[j]);
                }
                in = minicArray;
            } else if (in instanceof int[]) {
                // if calling with int[] from java mod this on the first call so users of the languate don't need to cast themselves
                int[] rawArray = (int[]) in;
                MinicIntArray minicArray = new MinicIntArray(new int[]{rawArray.length});
                for (int j = 0; j < rawArray.length; j++) {
                    minicArray.setAtPos(new int[]{j}, rawArray[j]);
                }
                in = minicArray;
            } else if (in instanceof String[]) {
                // if calling with int[] from java mod this on the first call so users of the languate don't need to cast themselves
                String[] rawArray = (String[]) in;
                MinicStringArray minicArray = new MinicStringArray(new int[]{rawArray.length});
                for (int j = 0; j < rawArray.length; j++) {
                    minicArray.setAtPos(new int[]{j}, rawArray[j]);
                }
                in = minicArray;
            }

            newInputs[i] = in;
        }

        return newInputs;
    }

    @Override
    public Object fromLanguage(Object result) {
        if (result instanceof MinicCharArray) {
            return JavaAssistUtil.safeFieldAccess("array", result);
        } else if (result instanceof MinicDoubleArray) {
            return JavaAssistUtil.safeFieldAccess("array", result);
        } else if (result instanceof MinicFloatArray) {
            return JavaAssistUtil.safeFieldAccess("array", result);
        } else if (result instanceof MinicIntArray) {
            return JavaAssistUtil.safeFieldAccess("array", result);
        } else if (result instanceof MinicStringArray) {
            return JavaAssistUtil.safeFieldAccess("array", result);
        }

        return result;
    }
}
