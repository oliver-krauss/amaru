/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package science.aist.seshat;

/**
 * Class containing configuration for values used in the logging
 * WARNING: WHENEVER YOU UPDATE GRAAL; JAVA; ETC: CHECK IF THIS STILL WORKS
 * @author Oliver Krauss on 12.12.2019
 */
public class LogConfiguration {

    public static final String LOG_LOCATION = System.getenv("LOG_LOC") != null ? System.getenv("LOG_LOC") : System.getProperty("user.dir") + "/logs/";

}
