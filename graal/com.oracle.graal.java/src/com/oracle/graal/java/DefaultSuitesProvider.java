/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.java;

import com.oracle.graal.lir.phases.LIRSuites;
import com.oracle.graal.nodes.graphbuilderconf.GraphBuilderConfiguration;
import com.oracle.graal.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import com.oracle.graal.phases.PhaseSuite;
import com.oracle.graal.phases.tiers.CompilerConfiguration;
import com.oracle.graal.phases.tiers.HighTierContext;
import com.oracle.graal.phases.tiers.Suites;

public class DefaultSuitesProvider extends SuitesProviderBase {

    private final CompilerConfiguration compilerConfiguration;

    public DefaultSuitesProvider(CompilerConfiguration compilerConfiguration, Plugins plugins) {
        super();
        this.defaultGraphBuilderSuite = createGraphBuilderSuite(plugins);
        this.compilerConfiguration = compilerConfiguration;
    }

    @Override
    public Suites createSuites() {
        return Suites.createSuites(compilerConfiguration);
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite(Plugins plugins) {
        PhaseSuite<HighTierContext> suite = new PhaseSuite<>();
        suite.appendPhase(new GraphBuilderPhase(GraphBuilderConfiguration.getDefault(plugins)));
        return suite;
    }

    @Override
    public LIRSuites createLIRSuites() {
        return Suites.createLIRSuites(compilerConfiguration);
    }
}
