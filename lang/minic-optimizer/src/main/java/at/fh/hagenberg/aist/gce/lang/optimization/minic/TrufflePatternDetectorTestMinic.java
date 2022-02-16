/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.lang.optimization.minic;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;

import java.io.IOException;

/**
 * Helper class so I can load the MINIC classes into the classpath
 *
 * @author Oliver Krauss on 28.11.2018
 */

public class TrufflePatternDetectorTestMinic {

    public static void main(String[] args) throws IOException {
        BitwisePatternMeta meta = new BitwisePatternMeta(TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID));
        // TODO #162 do a pattern detection for Minic
    }
}
