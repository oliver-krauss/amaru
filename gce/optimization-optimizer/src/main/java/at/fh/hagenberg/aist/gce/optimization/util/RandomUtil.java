/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import java.util.Random;

/**
 * This random util should be used in every class needing to create random values, as we can force the seed centrally.
 *
 * @author Oliver Krauss on 07.11.2018 // TODO #63 replace with machinelearning RandomUtil
 */
public class RandomUtil {

    public static Random random = new Random();

}
