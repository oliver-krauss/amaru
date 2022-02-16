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
 * Defines how patterns will be searched according to their significance.
 * @author Oliver Krauss on 07.10.2019
 */
public enum SignificanceType {

    MIN, // only the smallest pattern will be returned
         // Ex.: a occurs in two trees and a->b also occurs in both (but not a third!) only a will be returned (and obviously also b)
         // This returns only the core components of patterns, but not the patterns themselves

    MAX, // only the largest pattern will be returned
         // Ex.: a->b->c occurs in two trees, and a->b also occurs in both (but not a third!) only a->b->c will be returned
         // Essentially this will skip patterns that are just "on the way" to larger patterns

    ALL // all patterns will be returned

}
