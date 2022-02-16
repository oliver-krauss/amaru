/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.load;

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;

import java.io.Reader;

/**
 * Loader for Datasets (not reports)
 * @author Oliver Krauss on 23.10.2019
 */
public interface Loader {

    /**
     * Loads a dataset for processing
     * @return Dataset
     */
    Dataset load();

    /**
     * Reader that the dataset will be read from
     * @return Reader
     */
    Reader getReader();

    /**
     * Reader that the dataset will be read from
     * @param reader Reader
     */
    void setReader(Reader reader);
}
