/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.language;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.ObjectType;

/**
 * Class allowing checks if a given object is part of MiniC
 * Created by Oliver Krauss on 16.06.2016.
 */
public final class MinicObjectType extends ObjectType {

    public static boolean isInstance(TruffleObject obj) {
        return MinicContext.isMinicObject(obj);
    }

}
