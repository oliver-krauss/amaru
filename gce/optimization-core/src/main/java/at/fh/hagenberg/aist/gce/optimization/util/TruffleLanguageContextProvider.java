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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for what a valid truffle language context is
 * Created by Oliver Krauss on 23.02.2017.
 */
public interface TruffleLanguageContextProvider {

    /**
     * Provides information about which nodes are valid and instantiable in this context.
     * @return all classes available
     */
    Map<Class, TruffleClassInformation> getInstantiableNodes();

    /**
     * Provides a class mapping of all instantiable operators (non-terminals) for a given class
     * @return all operators
     */
    Map<Class, List<Class>> getOperators();

    /**
     * Provides a class mapping of all instantiable operands (terminals) for a given class
     * @return all operators
     */
    Map<Class, List<Class>> getOperands();

}
