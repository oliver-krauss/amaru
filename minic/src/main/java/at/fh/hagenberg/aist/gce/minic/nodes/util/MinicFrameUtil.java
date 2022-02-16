/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.util;

import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import com.oracle.truffle.api.frame.*;

/**
 * Utility class for reading variables upwards in scope
 * Created by Oliver Krauss on 15.06.2016.
 */
public class MinicFrameUtil {


    public static char getChar(Frame frame, FrameSlot slot) {
        return (char) FrameUtil.getObjectSafe(frame, slot);
    }

    public static int getInt(Frame frame, FrameSlot slot) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    public static float getFloat(Frame frame, FrameSlot slot) {
        return FrameUtil.getFloatSafe(frame, slot);
    }

    public static double getDouble(Frame frame, FrameSlot slot) {
        return FrameUtil.getDoubleSafe(frame, slot);
    }

    public static String getString(Frame frame, FrameSlot slot) {
        return (String) FrameUtil.getObjectSafe(frame, slot);
    }

    public static char getChar(Frame frame, FrameSlot slot, int[] position) {
        return ((MinicCharArray) FrameUtil.getObjectSafe(frame, slot)).getAtPos(position);
    }

    public static int getInt(Frame frame, FrameSlot slot, int[] position) {
        return ((MinicIntArray) FrameUtil.getObjectSafe(frame, slot)).getAtPos(position);
    }

    public static float getFloat(Frame frame, FrameSlot slot, int[] position) {
        return ((MinicFloatArray) FrameUtil.getObjectSafe(frame, slot)).getAtPos(position);
    }

    public static double getDouble(Frame frame, FrameSlot slot, int[] position) {
        return ((MinicDoubleArray) FrameUtil.getObjectSafe(frame, slot)).getAtPos(position);
    }

    public static String getString(Frame frame, FrameSlot slot, int[] position) {
        return ((MinicStringArray) FrameUtil.getObjectSafe(frame, slot)).getAtPos(position);
    }

    public static Object getArray(Frame frame, FrameSlot slot) {
        return FrameUtil.getObjectSafe(frame, slot);
    }
}