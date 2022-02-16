/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

/**
 * A strategy defines how truffle values (both nodes as well as terminal values!) will be created
 * These include for example int, char, java.lang.String
 *
 * @param <T> the object to be created (either a Node or also a terminal such as int, char, ...)
 */
public interface TruffleCombinedStrategy<T> extends TruffleHierarchicalStrategy<T>, TruffleSimpleStrategy<T> {
}
