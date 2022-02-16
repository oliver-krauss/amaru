/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.test;


import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 21.11.2018
 */

public class TruffleTestValueTest {

    @Test
    public void testAccuracyInt() {
        // given
        TruffleTestValue zero = new TruffleTestValue(0, "int");
        TruffleTestValue one = new TruffleTestValue(1, "int");
        TruffleTestValue two = new TruffleTestValue(2, "int");
        TruffleTestValue a = new TruffleTestValue(-7, "int");
        TruffleTestValue b = new TruffleTestValue(7, "int");
        TruffleTestValue c = new TruffleTestValue(3, "int");
        TruffleTestValue d = new TruffleTestValue(-3, "int");

        // when
        double equal = zero.compare(zero);
        double minimal = zero.compare(one);
        double dual = zero.compare(two);
        double r1 = a.compare(a);
        double r2 = a.compare(b);
        double r3 = a.compare(c);
        double r4 = a.compare(d);
        double r5 = b.compare(b);
        double r6 = b.compare(c);
        double r7 = b.compare(d);
        double r8 = c.compare(c);
        double r9 = c.compare(d);
        double r0 = d.compare(d);

        // then
        Assert.assertEquals(equal, 0, 0);
        Assert.assertTrue(minimal > 0);
        Assert.assertTrue(dual > 0);
        Assert.assertTrue(equal < minimal && minimal < dual);
        Assert.assertTrue(r1 >= 0 && r1 <= 1);
        Assert.assertTrue(r2 >= 0 && r2 <= 1);
        Assert.assertTrue(r3 >= 0 && r3 <= 1);
        Assert.assertTrue(r4 >= 0 && r4 <= 1);
        Assert.assertTrue(r5 >= 0 && r5 <= 1);
        Assert.assertTrue(r6 >= 0 && r6 <= 1);
        Assert.assertTrue(r7 >= 0 && r7 <= 1);
        Assert.assertTrue(r8 >= 0 && r8 <= 1);
        Assert.assertTrue(r9 >= 0 && r9 <= 1);
        Assert.assertTrue(r0 >= 0 && r0 <= 1);
    }

    @Test
    public void testAccuracyChar() {
        // given
        TruffleTestValue zero = new TruffleTestValue('0', "char");
        TruffleTestValue one = new TruffleTestValue('1', "char");
        TruffleTestValue two = new TruffleTestValue('2', "char");
        TruffleTestValue a = new TruffleTestValue('#', "char");
        TruffleTestValue b = new TruffleTestValue('§', "char");
        TruffleTestValue c = new TruffleTestValue('a', "char");
        TruffleTestValue d = new TruffleTestValue('A', "char");

        // when
        double equal = zero.compare(zero);
        double minimal = zero.compare(one);
        double dual = zero.compare(two);
        double r1 = a.compare(a);
        double r2 = a.compare(b);
        double r3 = a.compare(c);
        double r4 = a.compare(d);
        double r5 = b.compare(b);
        double r6 = b.compare(c);
        double r7 = b.compare(d);
        double r8 = c.compare(c);
        double r9 = c.compare(d);
        double r0 = d.compare(d);

        // then
        Assert.assertEquals(equal, 0, 0);
        Assert.assertTrue(minimal > 0);
        Assert.assertTrue(dual > 0);
        Assert.assertTrue(equal < minimal && minimal < dual);
        Assert.assertTrue(r1 >= 0 && r1 <= 1);
        Assert.assertTrue(r2 >= 0 && r2 <= 1);
        Assert.assertTrue(r3 >= 0 && r3 <= 1);
        Assert.assertTrue(r4 >= 0 && r4 <= 1);
        Assert.assertTrue(r5 >= 0 && r5 <= 1);
        Assert.assertTrue(r6 >= 0 && r6 <= 1);
        Assert.assertTrue(r7 >= 0 && r7 <= 1);
        Assert.assertTrue(r8 >= 0 && r8 <= 1);
        Assert.assertTrue(r9 >= 0 && r9 <= 1);
        Assert.assertTrue(r0 >= 0 && r0 <= 1);
    }

    @Test
    public void testDoubleAccuracy() {
        // given
        TruffleTestValue zero = new TruffleTestValue(0.0, "double");
        TruffleTestValue one = new TruffleTestValue(0.000000000000001, "double");
        TruffleTestValue two = new TruffleTestValue(0.000000000000002, "double");
        TruffleTestValue a = new TruffleTestValue(-7.0, "double");
        TruffleTestValue b = new TruffleTestValue(7.0, "double");
        TruffleTestValue c = new TruffleTestValue(3.0, "double");
        TruffleTestValue d = new TruffleTestValue(-3.0, "double");

        // when
        double equal = zero.compare(zero);
        double minimal = zero.compare(one);
        double dual = zero.compare(two);
        double r1 = a.compare(a);
        double r2 = a.compare(b);
        double r3 = a.compare(c);
        double r4 = a.compare(d);
        double r5 = b.compare(b);
        double r6 = b.compare(c);
        double r7 = b.compare(d);
        double r8 = c.compare(c);
        double r9 = c.compare(d);
        double r0 = d.compare(d);

        // then
        Assert.assertEquals(equal, 0, 0);
        Assert.assertTrue(minimal > 0);
        Assert.assertTrue(dual > 0);
        Assert.assertTrue(equal < minimal && minimal < dual);
        Assert.assertTrue(r1 >= 0 && r1 <= 1);
        Assert.assertTrue(r2 >= 0 && r2 <= 1);
        Assert.assertTrue(r3 >= 0 && r3 <= 1);
        Assert.assertTrue(r4 >= 0 && r4 <= 1);
        Assert.assertTrue(r5 >= 0 && r5 <= 1);
        Assert.assertTrue(r6 >= 0 && r6 <= 1);
        Assert.assertTrue(r7 >= 0 && r7 <= 1);
        Assert.assertTrue(r8 >= 0 && r8 <= 1);
        Assert.assertTrue(r9 >= 0 && r9 <= 1);
        Assert.assertTrue(r0 >= 0 && r0 <= 1);
    }

    @Test
    public void testFloatAccuracy() {
        // given
        TruffleTestValue zero = new TruffleTestValue(0.0f, "float");
        TruffleTestValue one = new TruffleTestValue(0.00000000000000000000000000000000000000000001f, "float");
        TruffleTestValue two = new TruffleTestValue(0.00000000000000000000000000000000000000000002f, "float");
        TruffleTestValue a = new TruffleTestValue(-7.0f, "float");
        TruffleTestValue b = new TruffleTestValue(7.0f, "float");
        TruffleTestValue c = new TruffleTestValue(3.0f, "float");
        TruffleTestValue d = new TruffleTestValue(-3.0f, "float");

        // when
        double equal = zero.compare(zero);
        double minimal = zero.compare(one);
        double dual = zero.compare(two);
        double r1 = a.compare(a);
        double r2 = a.compare(b);
        double r3 = a.compare(c);
        double r4 = a.compare(d);
        double r5 = b.compare(b);
        double r6 = b.compare(c);
        double r7 = b.compare(d);
        double r8 = c.compare(c);
        double r9 = c.compare(d);
        double r0 = d.compare(d);

        // then
        Assert.assertEquals(equal, 0, 0);
        Assert.assertTrue(minimal > 0);
        Assert.assertTrue(dual > 0);
        Assert.assertTrue(equal < minimal && minimal < dual);
        Assert.assertTrue(r1 >= 0 && r1 <= 1);
        Assert.assertTrue(r2 >= 0 && r2 <= 1);
        Assert.assertTrue(r3 >= 0 && r3 <= 1);
        Assert.assertTrue(r4 >= 0 && r4 <= 1);
        Assert.assertTrue(r5 >= 0 && r5 <= 1);
        Assert.assertTrue(r6 >= 0 && r6 <= 1);
        Assert.assertTrue(r7 >= 0 && r7 <= 1);
        Assert.assertTrue(r8 >= 0 && r8 <= 1);
        Assert.assertTrue(r9 >= 0 && r9 <= 1);
        Assert.assertTrue(r0 >= 0 && r0 <= 1);
    }

    @Test
    public void testStringAccuracy() {
        // given
        TruffleTestValue zero = new TruffleTestValue("HELLO WORLD!", "java.lang.String");
        TruffleTestValue one = new TruffleTestValue("IELLO WORLD!", "java.lang.String");
        TruffleTestValue two = new TruffleTestValue("JELLY WORLD!", "java.lang.String");
        TruffleTestValue a = new TruffleTestValue("Hello my name is Slim Shady", "java.lang.String");
        TruffleTestValue b = new TruffleTestValue("What is a name?", "java.lang.String");
        TruffleTestValue c = new TruffleTestValue("No clue!", "java.lang.String");
        TruffleTestValue d = new TruffleTestValue("- Levy Levenstein", "java.lang.String");

        // when
        double equal = zero.compare(zero);
        double minimal = zero.compare(one);
        double dual = zero.compare(two);
        double r1 = a.compare(a);
        double r2 = a.compare(b);
        double r3 = a.compare(c);
        double r4 = a.compare(d);
        double r5 = b.compare(b);
        double r6 = b.compare(c);
        double r7 = b.compare(d);
        double r8 = c.compare(c);
        double r9 = c.compare(d);
        double r0 = d.compare(d);

        // then
        Assert.assertEquals(equal, 0, 0);
        ;
        Assert.assertTrue(minimal > 0);
        Assert.assertTrue(dual > 0);
        Assert.assertTrue(equal < minimal && minimal < dual);
        Assert.assertTrue(r1 >= 0 && r1 <= 1);
        Assert.assertTrue(r2 >= 0 && r2 <= 1);
        Assert.assertTrue(r3 >= 0 && r3 <= 1);
        Assert.assertTrue(r4 >= 0 && r4 <= 1);
        Assert.assertTrue(r5 >= 0 && r5 <= 1);
        Assert.assertTrue(r6 >= 0 && r6 <= 1);
        Assert.assertTrue(r7 >= 0 && r7 <= 1);
        Assert.assertTrue(r8 >= 0 && r8 <= 1);
        Assert.assertTrue(r9 >= 0 && r9 <= 1);
        Assert.assertTrue(r0 >= 0 && r0 <= 1);
    }

}
