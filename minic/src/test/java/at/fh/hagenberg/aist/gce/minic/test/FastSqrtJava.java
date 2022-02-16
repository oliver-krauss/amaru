/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.test;

/**
 * Created from online sources to see if we can reproduce the "exponent extraction" trick within MiniC
 * Problem is that MiniC is currently missing way too many concepts for this (all bit operations)
 */
public class FastSqrtJava {

    //Magic numbers used for computing
    // best guess starting square
    static final float A = 0.417319242f;
    static final float B = 0.590178532f;
    static final float factor = (float) Math.sqrt(0.5);

    public static void main(String[] args) {

        fastSqrt(0.5);
        fastSqrt(1.0);
        fastSqrt(2.0);
        fastSqrt(3.0);
        fastSqrt(4.0);
        fastSqrt(5.0);
        fastSqrt(5000.0);
        fastSqrt(5000000.0);
        fastSqrt(5000000000.0);
        System.out.println(fastSqrt(3.4028234664E+18));
        double val = 0.0;
        for (int i = 0; i < 1000; i++) {
            val = i * 0.01;
            fastSqrt(val);
            System.out.println("SQUIRT " + (fastSqrt(val) - Math.sqrt(val)));
        }
    }

    static public double fastSqrt(double fp) {
        if (fp <= 0) return 0;
        int expo;
        double root;
        // split into hi and lo bits
        long bitValue = Double.doubleToRawLongBits(fp);
        int lo = (int) (bitValue);
        int hi = (int) (bitValue >> 32);

        // pull out exponent and format
        expo = hi >> 20;
        expo -= 0x3fe;
        // clear exponent and set "normalized" bits
        hi &= 0x000fffff;
        hi += 0x3fe00000;
        // assemble back to double bits
        bitValue = ((long) (hi) << 32) + lo;
        // turn bitValue back into decimal float for more processing
        fp = Double.longBitsToDouble(bitValue);

        // find square for decimal using Magic numbers best guess
        root = A + B * fp;
        // repeat for more accuracy, but slower function
        root = 0.5 * (fp / root + root);
        root = 0.5 * (fp / root + root);
        root = 0.5 * (fp / root + root);
        //root = 0.5 * (fp/root + root);
        // root = 0.5 * (fp/root + root);
        fp = root;
        // find square for expo
        if ((expo & 1) != 0) {
            fp *= factor;
            ++expo;
        }
        if (expo < 0) expo = (short) (expo / 2);
        else
            expo = (short) ((expo + 1) / 2);
        // format back for floating point
        expo += 0x3fe;
        // split back to hi and lo bits
        bitValue = Double.doubleToLongBits(fp);
        lo = (int) (bitValue);
        hi = (int) (bitValue >> 32);
        // put in exponent
        hi &= 0x000fffff;
        hi += expo << 20;
        // assemble back to double bits
        bitValue = ((long) (hi) << 32) + lo;
        fp = Double.longBitsToDouble(bitValue);
        return fp;
    }

    static final public float fastSqrtF(float fp) {
        if (fp <= 0) return 0;
        int expo;
        float root;
        // convert float to bit representation
        int bitValue = Float.floatToRawIntBits(fp);
        // pull out exponent and format
        expo = (bitValue >> 23);
        // subtract exponent bias
        expo -= 126;
        // clear exponent in bitValue
        bitValue &= 0x7fffff;
        // sets expo bits to 127
        // which is really 1 with the base of 126
        // effective making the number decimal only
        bitValue |= 0x3F800000; // ( 127 << 23 )
        // turn bitValue back into decimal float for more processing
        fp = Float.intBitsToFloat(bitValue);
        // find square for decimal using Magic numbers best guess
        root = A + B * fp;
        // repeat for more accuracy, but slower function
        root = 0.5f * (fp / root + root);
        root = 0.5f * (fp / root + root);
        root = 0.5f * (fp / root + root);
        // find square for expo
        if ((expo & 1) == 0) {
            root *= factor;
        }
        expo++;
        expo >>= 1;
        // format back for floating point
        expo += 126;
        // once again turn back to bits for recombining
        bitValue = Float.floatToRawIntBits(root);
        // clear exponent in bitValue
        bitValue &= 0x7fffff;
        // put in exponent into bitValue
        bitValue += (expo << 23);
        return Float.intBitsToFloat(bitValue);
    }

}
