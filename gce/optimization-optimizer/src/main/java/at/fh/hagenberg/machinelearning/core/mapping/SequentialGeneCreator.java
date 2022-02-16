/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.core.mapping;

/**
 * The sequential gene creator guarantees that it will create a gene not randomly, but according to a sequence (specific to a use-case)
 * The sequence can consist of unique values (but doesn't have to!), the sequence should be identical for different creations of a specific PT gene
 *
 * @param <ST> Solution type
 * @param <PT> Problem type
 */
public interface SequentialGeneCreator<ST, PT> extends at.fh.hagenberg.machinelearning.core.mapping.GeneCreator<ST, PT> {

    /**
     * Checks if there is a next instance in the queue.
     * If there is not, then the next returned gene will be the same as the first one from the queue
     *
     * @return if there is a next instance in the sequence that can be returned
     */
    public boolean hasNext();

}
