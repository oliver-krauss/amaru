/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.selection;

import at.fh.hagenberg.aist.gce.pattern.algorithm.StringSubgraphIterator;
import at.fh.hagenberg.aist.gce.pattern.constraint.CachetConstraint;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 3) count the amount of permutations the iterator produces
 * The tests below were hand-checked and the amount of permutations is valid. It should NEVER change over all possible permute-implementations.
 *
 * @author Oliver Krauss on 02.10.2019
 */
@Test
public class StringSubgraphIteratorTest extends TestDBTest {

    @Test
    public void testMineT1() {
        permute(t1, 1, -1, true);
    }

    @Test
    public void testMineT2() {
        permute(t2, 6, -1, true);
    }

    @Test
    public void testMineT3() {
        permute(t3, 8, 1, true);
    }

    @Test
    public void testMineT3D2() {
        permute(t3, 8 + 7, 2, true);
    }

    @Test
    public void testMineT3D3() {
        permute(t3, 8 + 7 + 9, 3, true);
    }

    @Test
    public void testMineT3D4() {
        permute(t3, 8 + 7 + 9 + 11, 4, true);
    }

    @Test
    public void testMineT3D5() {
        permute(t3, 8 + 7 + 9 + 11 + 12, 5, true);
    }

    @Test
    public void testMineT3D6() {
        permute(t3, 8 + 7 + 9 + 11 + 12 + 10, 6, true);
    }

    @Test
    public void testMineT3D7() {
        permute(t3, 8 + 7 + 9 + 11 + 12 + 10 + 5, 7, true);
    }

    @Test
    public void testMineT3D8() {
        permute(t3, 8 + 7 + 9 + 11 + 12 + 10 + 5 + 1, 8, true);
    }

    @Test
    public void testMineT3D9() {
        permute(t3, 8 + 7 + 9 + 11 + 12 + 10 + 5 + 1, 9, true);
    }

    private List<NodeWrapper> permute(NodeWrapper node, int size, int patternSize, boolean explicit) {
        // given
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        definition.includeTree(node);
        TrufflePatternSearchSpace trees = repository.findTrees(definition);

        // when
        StringSubgraphIterator iterator = new StringSubgraphIterator(trees, patternSize, explicit);
        List<NodeWrapper> permutations = new LinkedList<>();
        int i = 0;
        while (iterator.hasNext()) {
            NodeWrapper next = iterator.next();
            if (next != null) {
                permutations.add(next);
                i++;
            }
        }

        // then
        Assert.assertNotNull(iterator);
        Assert.assertEquals(i, size);

        return permutations;
    }


}
