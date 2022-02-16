/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern;

/**
 * Defines how patterns will be searched by the specialization or generalization level in the hierarchy.
 * @author Oliver Krauss on 04.03.2021
 */
public enum SpecializationType {

    SPECIALIZED, // only the most spezialized pattern will be returned
    // Ex.: two patterns with the structure a->b->c occur in the exact same locations, a, and b are equivalent
    //      and c exists a IntAddNode and AddNode. Specialized will return only the IntAddNode version
    // Essentially this will skip patterns that are "overgeneralized"

    GENERALIZED, // only the most generalized pattern will be returned
    // Ex.: two patterns with the structure a->b->c occur in the exact same locations, a, and b are equivalent
    //      and c exists a IntAddNode and AddNode. Specialized will return only the AddNode version
    // Essentially this will skip specialized patterns.

    ALL // all patterns will be returned
}
