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

import at.fh.hagenberg.aist.gce.science.statistics.data.template.Transformer;

import java.io.File;

/**
 * Interface to be used for the {@link FileStore} to do processing in saving the files
 * @author Oliver Krauss on 24.10.2019
 */
public interface FileProcessor {

    /**
     * Anything to be done before the transformation is applied.
     * @param file        that is the storage target
     * @param transformer that will be called
     */
    public void preProcessing(File file, Transformer transformer);

    /**
     * Anything to be done after the transformation is applied.
     * @param file        that is the storage target
     * @param transformer that will be called
     */
    public void postProcessing(File file, Transformer transformer);
}
