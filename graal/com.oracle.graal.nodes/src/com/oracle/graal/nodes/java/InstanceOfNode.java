/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.java;

import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.TriState;

import com.oracle.graal.compiler.common.type.TypeReference;
import com.oracle.graal.compiler.common.type.ObjectStamp;
import com.oracle.graal.compiler.common.type.Stamp;
import com.oracle.graal.compiler.common.type.StampFactory;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.LogicConstantNode;
import com.oracle.graal.nodes.LogicNegationNode;
import com.oracle.graal.nodes.LogicNode;
import com.oracle.graal.nodes.UnaryOpLogicNode;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.calc.IsNullNode;
import com.oracle.graal.nodes.spi.Lowerable;
import com.oracle.graal.nodes.spi.LoweringTool;
import com.oracle.graal.nodes.spi.Virtualizable;
import com.oracle.graal.nodes.spi.VirtualizerTool;
import com.oracle.graal.nodes.type.StampTool;

/**
 * The {@code InstanceOfNode} represents an instanceof test.
 */
@NodeInfo
public class InstanceOfNode extends UnaryOpLogicNode implements Lowerable, Virtualizable {
    public static final NodeClass<InstanceOfNode> TYPE = NodeClass.create(InstanceOfNode.class);

    protected final ObjectStamp checkedStamp;
    protected JavaTypeProfile profile;

    private InstanceOfNode(ObjectStamp checkedStamp, ValueNode object, JavaTypeProfile profile) {
        this(TYPE, checkedStamp, object, profile);
    }

    protected InstanceOfNode(NodeClass<? extends InstanceOfNode> c, ObjectStamp checkedStamp, ValueNode object, JavaTypeProfile profile) {
        super(c, object);
        this.checkedStamp = checkedStamp;
        this.profile = profile;
        assert checkedStamp != null;
    }

    public static LogicNode create(TypeReference type, ValueNode object, JavaTypeProfile profile) {
        ObjectStamp checkedStamp = StampFactory.objectNonNull(type);
        LogicNode synonym = findSynonym(checkedStamp, object);
        if (synonym != null) {
            return synonym;
        } else {
            return new InstanceOfNode(checkedStamp, object, profile);
        }
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        LogicNode synonym = findSynonym(checkedStamp, forValue);
        if (synonym != null) {
            return synonym;
        } else {
            return this;
        }
    }

    public static LogicNode findSynonym(ObjectStamp checkedStamp, ValueNode object) {
        ObjectStamp inputStamp = (ObjectStamp) object.stamp();
        ObjectStamp joinedStamp = (ObjectStamp) checkedStamp.join(inputStamp);

        if (joinedStamp.isEmpty()) {
            // The check can never succeed, the intersection of the two stamps is empty.
            return LogicConstantNode.contradiction();
        } else {
            ObjectStamp meetStamp = (ObjectStamp) checkedStamp.meet(inputStamp);
            if (checkedStamp.equals(meetStamp)) {
                // The check will always succeed, the union of the two stamps is equal to the
                // checked stamp.
                return LogicConstantNode.tautology();
            } else if (checkedStamp.type().equals(meetStamp.type()) && checkedStamp.isExactType() == meetStamp.isExactType() && checkedStamp.alwaysNull() == meetStamp.alwaysNull()) {
                assert checkedStamp.nonNull() && !meetStamp.nonNull() && !inputStamp.nonNull();
                // The only difference makes the null-ness of the value => simplify the check.
                // The instanceof is true if the input is non-null.
                return LogicNegationNode.create(new IsNullNode(object));
            }
        }

        return null;
    }

    /**
     * Gets the type being tested.
     */
    public TypeReference type() {
        return StampTool.typeReferenceOrNull(checkedStamp);
    }

    public JavaTypeProfile profile() {
        return profile;
    }

    public void setProfile(JavaTypeProfile profile) {
        this.profile = profile;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(getValue());
        TriState fold = tryFold(alias.stamp());
        if (fold != TriState.UNKNOWN) {
            tool.replaceWithValue(LogicConstantNode.forBoolean(fold.isTrue(), graph()));
        }
    }

    @Override
    public Stamp getSucceedingStampForValue(boolean negated) {
        if (negated) {
            return null;
        } else {
            return checkedStamp;
        }
    }

    @Override
    public TriState tryFold(Stamp valueStamp) {
        if (valueStamp instanceof ObjectStamp) {
            ObjectStamp inputStamp = (ObjectStamp) valueStamp;
            ObjectStamp joinedStamp = (ObjectStamp) checkedStamp.join(inputStamp);

            if (joinedStamp.isEmpty()) {
                // The check can never succeed, the intersection of the two stamps is empty.
                return TriState.FALSE;
            } else {
                ObjectStamp meetStamp = (ObjectStamp) checkedStamp.meet(inputStamp);
                if (checkedStamp.equals(meetStamp)) {
                    // The check will always succeed, the union of the two stamps is equal to the
                    // checked stamp.
                    return TriState.TRUE;
                }
            }
        }
        return TriState.UNKNOWN;
    }
}
