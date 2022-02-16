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
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simply stores to the required file
 *
 * @author Oliver Krauss on 24.10.2019
 */
public class StoreProcessor implements FileProcessor {

    @Override
    public void preProcessing(File file, Transformer transformer) {
        try {
            FileWriter writer = new FileWriter(file);
            transformer.setWriter(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postProcessing(File file, Transformer transformer) {
        try {
            transformer.getWriter().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
