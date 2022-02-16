/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.store;

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;

/**
 * Storage for Datasets and Reports generated out of them
 * @author Oliver Krauss on 23.10.2019
 */
public interface Store {

    /**
     * Loads a dataset for processing
     * @return Dataset
     */
    Dataset load();

    /**
     * Stores a dataset
     * @param dataset to be stored
     */
    void save(Dataset dataset);

    /**
     * Stores a report
     * @param report to be stored (incl. sub-reports)
     */
    void save(Report report);

    /**
     * Stores a dataset with its corresponding report
     * @param dataset to be stored
     * @param report to be stored
     */
    void save(Dataset dataset, Report report);
}
