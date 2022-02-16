/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.machinelearning.analytics.TruffleGraphAnalytics;

/**
 * @author Oliver Krauss on 08.01.2020
 */
public interface Analyzable {

    /**
     * Setter for the analytics service if you want this class to log to the Database
     * If the setter MUST happen, annotate the value with @Required
     * @param analyticsService
     */
    void setAnalyticsService(TruffleGraphAnalytics analyticsService);
}
