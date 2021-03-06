/*
// Licensed to DynamoBI Corporation (DynamoBI) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  DynamoBI licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at

//   http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
*/
package org.eigenbase.rel;

import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.rex.*;


/**
 * <code>FilterRelBase</code> is an abstract base class for implementations of
 * {@link FilterRel}.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public abstract class FilterRelBase
    extends SingleRel
{
    //~ Instance fields --------------------------------------------------------

    private final RexNode condition;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a filter.
     *
     * @param cluster {@link RelOptCluster}  this relational expression belongs
     * to
     * @param traits the traits of this rel
     * @param child input relational expression
     * @param condition boolean expression which determines whether a row is
     * allowed to pass
     */
    protected FilterRelBase(
        RelOptCluster cluster,
        RelTraitSet traits,
        RelNode child,
        RexNode condition)
    {
        super(cluster, traits, child);
        this.condition = condition;
    }

    //~ Methods ----------------------------------------------------------------

    public RexNode [] getChildExps()
    {
        return new RexNode[] { condition };
    }

    public RexNode getCondition()
    {
        return condition;
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        double dRows = RelMetadataQuery.getRowCount(this);
        double dCpu = RelMetadataQuery.getRowCount(getChild());
        double dIo = 0;
        return planner.makeCost(dRows, dCpu, dIo);
    }

    // override RelNode
    public double getRows()
    {
        return estimateFilteredRows(
            getChild(),
            condition);
    }

    public static double estimateFilteredRows(RelNode child, RexProgram program)
    {
        // convert the program's RexLocalRef condition to an expanded RexNode
        RexLocalRef programCondition = program.getCondition();
        RexNode condition;
        if (programCondition == null) {
            condition = null;
        } else {
            condition = program.expandLocalRef(programCondition);
        }
        return estimateFilteredRows(
            child,
            condition);
    }

    public static double estimateFilteredRows(RelNode child, RexNode condition)
    {
        return RelMetadataQuery.getRowCount(child)
            * RelMetadataQuery.getSelectivity(child, condition);
    }

    public void explain(RelOptPlanWriter pw)
    {
        pw.explain(
            this,
            new String[] { "child", "condition" });
    }
}

// End FilterRelBase.java
