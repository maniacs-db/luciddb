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
package org.eigenbase.rel.rules;

import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.rex.*;


/**
 * MergeProjectRule merges a {@link ProjectRel} into another {@link ProjectRel},
 * provided the projects aren't projecting identical sets of input references.
 *
 * @author Zelaine Fong
 * @version $Id$
 */
public class MergeProjectRule
    extends RelOptRule
{
    public static final MergeProjectRule instance =
        new MergeProjectRule();

    //~ Instance fields --------------------------------------------------------

    /**
     * if true, always merge projects
     */
    private final boolean force;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a MergeProjectRule.
     */
    private MergeProjectRule()
    {
        this(false);
    }

    /**
     * Creates a MergeProjectRule, specifying whether to always merge projects.
     *
     * @param force Whether to always merge projects
     */
    public MergeProjectRule(boolean force)
    {
        super(
            new RelOptRuleOperand(
                ProjectRel.class,
                new RelOptRuleOperand(ProjectRel.class, ANY)),
            "MergeProjectRule" + (force ? ": force mode" : ""));
        this.force = force;
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptRule
    public void onMatch(RelOptRuleCall call)
    {
        ProjectRel topProject = (ProjectRel) call.rels[0];
        ProjectRel bottomProject = (ProjectRel) call.rels[1];
        RexBuilder rexBuilder = topProject.getCluster().getRexBuilder();

        // if we're not in force mode and the two projects reference identical
        // inputs, then return and either let FennelRenameRule or
        // RemoveTrivialProjectRule replace the projects
        if (!force) {
            if (RelOptUtil.checkProjAndChildInputs(topProject, false)) {
                return;
            }
        }

        // create a RexProgram for the bottom project
        RexProgram bottomProgram =
            RexProgram.create(
                bottomProject.getChild().getRowType(),
                bottomProject.getProjectExps(),
                null,
                bottomProject.getRowType(),
                rexBuilder);

        // create a RexProgram for the topmost project
        RexNode [] projExprs = topProject.getProjectExps();
        RexProgram topProgram =
            RexProgram.create(
                bottomProject.getRowType(),
                projExprs,
                null,
                topProject.getRowType(),
                rexBuilder);

        // combine the two RexPrograms
        RexProgram mergedProgram =
            RexProgramBuilder.mergePrograms(
                topProgram,
                bottomProgram,
                rexBuilder);

        // re-expand the topmost projection expressions, now that they
        // reference the children of the bottom-most project
        int nProjExprs = projExprs.length;
        RexNode [] newProjExprs = new RexNode[nProjExprs];
        List<RexLocalRef> projList = mergedProgram.getProjectList();
        for (int i = 0; i < nProjExprs; i++) {
            newProjExprs[i] = mergedProgram.expandLocalRef(projList.get(i));
        }

        // replace the two projects with a combined projection
        ProjectRel newProjectRel =
            (ProjectRel) CalcRel.createProject(
                bottomProject.getChild(),
                newProjExprs,
                RelOptUtil.getFieldNames(topProject.getRowType()));

        call.transformTo(newProjectRel);
    }
}

// End MergeProjectRule.java
