/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.template;

import at.fh.hagenberg.aist.gce.science.statistics.Report;
import at.fh.hagenberg.aist.gce.science.statistics.data.Dataset;

import java.util.HashMap;
import java.util.Map;

/**
 * Preprocessor for the latex templates
 *
 * @author Oliver Krauss on 24.10.2019
 */
public class LatexPreprocessor implements TemplatePreprocessor {

    @Override
    public Map<String, Object> process(Dataset ds, Report r) {
        Map<String, Object> values = new HashMap<>();

        // don't care about dataset

        // process report
        if (r.getName() != null) {
            values.put("caption", r.getName());
        }

        if (r.getValue("label") != null) {
            values.put("label", r.getValue("label"));
        }

        return values;
    }
}
