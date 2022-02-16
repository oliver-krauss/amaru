#
# Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

import mx
import os

from mx_gate import Task, add_gate_runner
#from mx_jvmci import VM, buildvms

_suite = mx.suite('minic')
_root = os.path.join(_suite.dir, "minic/")

def truffle_extract_VM_args(args, useDoubleDash=False):
    vmArgs, remainder, classpath = [], [], ""
    argIter = iter(enumerate(args))
    for (i, arg) in argIter:
        if any(arg.startswith(prefix) for prefix in ['-X', '-G:', '-D', '-verbose', '-ea']) or arg in ['-esa']:
            vmArgs += [arg]
        elif arg in ['-cp']:
            (i, arg) = next(argIter)
            classpath = arg
        elif useDoubleDash and arg == '--':
            remainder += args[i:]
            break
        else:
            remainder += [arg]

    return vmArgs, remainder, classpath

def set_default_vm():
    defaultVmKey = 'DEFAULT_VM'
    if mx.get_env(defaultVmKey) is None and not mx.is_interactive():
        os.environ[defaultVmKey] = 'jvmci'

mx.update_commands(_suite, {
    # new commands
  
    # core overrides
})

set_default_vm()

