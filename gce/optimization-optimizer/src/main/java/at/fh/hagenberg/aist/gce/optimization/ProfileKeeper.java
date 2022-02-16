/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization;

import at.fh.hagenberg.aist.gce.optimization.util.NanoProfiler;

/**
 * Simple profile keeper for analyzing the optimization tools
 * Please note that EVERYTHING RUN will be profiled and no separation of experiments is possible at the moment.
 */
public class ProfileKeeper {

    public static final NanoProfiler profiler = new NanoProfiler("Optimization", 10000);
}
