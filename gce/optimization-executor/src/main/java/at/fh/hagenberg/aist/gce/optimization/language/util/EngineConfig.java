/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language.util;

/**
 * Class containing configuration for values used in the remote engines (Console Workers)
 * WARNING: WHENEVER YOU UPDATE GRAAL; JAVA; ETC: CHECK IF THIS STILL WORKS
 * @author Oliver Krauss on 12.12.2019
 */
public class EngineConfig {

    public static final String ROOT_LOCATION = System.getenv("ROOT_LOCATION") != null ? System.getenv("ROOT_LOCATION") : System.getProperty("user.dir");

    public static final String JAVA_LOCATION = System.getenv("GRAAL_LOC") != null ? System.getenv("GRAAL_LOC") : "java";

    public static final String DIST_LOCATION = System.getenv("DIST_DIR") != null ? System.getenv("DIST_DIR") : System.getProperty("user.dir");

    public static final String JAVA_CALL_PARAMS = "--illegal-access=permit -Dgraalvm.locatorDisabled=true --add-exports org.graalvm.truffle/com.oracle.truffle.api=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.debug=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.dsl=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.frame=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.instrumentation=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.interop=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.io=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.library=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.nodes=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.object=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.object.dsl=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.profiles=ALL-UNNAMED --add-exports org.graalvm.truffle/com.oracle.truffle.api.source=ALL-UNNAMED --add-opens org.graalvm.sdk/org.graalvm.polyglot=ALL-UNNAMED --add-opens jdk.internal.vm.compiler/org.graalvm.compiler.truffle.runtime=ALL-UNNAMED --add-opens org.graalvm.truffle/com.oracle.truffle.api.nodes=ALL-UNNAMED --add-opens org.graalvm.truffle/com.oracle.truffle.api.dsl=ALL-UNNAMED --add-opens org.graalvm.truffle/com.oracle.truffle.api=ALL-UNNAMED";
}
