/*
 * Sonar Delphi Plugin
 * Copyright (C) 2019-2022 Integrated Application Development
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.communitydelphi.pmd.rules;

import static org.sonar.plugins.communitydelphi.utils.conditions.RuleKey.ruleKey;
import static org.sonar.plugins.communitydelphi.utils.conditions.RuleKeyAtLine.ruleKeyAtLine;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.communitydelphi.utils.builders.DelphiTestUnitBuilder;

class RedundantBooleanRuleTest extends BasePmdRuleTest {

  @Test
  void testRedundantBooleanComparisonShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure MyProcedure;")
            .appendImpl("var")
            .appendImpl("  X: Boolean;")
            .appendImpl("begin")
            .appendImpl("  if X = True then begin")
            .appendImpl("    DoSomething;")
            .appendImpl("  end;")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areExactly(1, ruleKeyAtLine("RedundantBooleanRule", builder.getOffset() + 5));
  }

  @Test
  void testBooleanComparisonImplicitConversionShouldNotAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure MyProcedure;")
            .appendImpl("var")
            .appendImpl("  X: Variant;")
            .appendImpl("begin")
            .appendImpl("  if X = True then begin")
            .appendImpl("    DoSomething;")
            .appendImpl("  end;")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areNot(ruleKey("RedundantBooleanRule"));
  }

  @Test
  void testRedundantBooleanNegativeComparisonShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure MyProcedure;")
            .appendImpl("var")
            .appendImpl("  X: Boolean;")
            .appendImpl("begin")
            .appendImpl("  if X <> False then begin")
            .appendImpl("    DoSomething;")
            .appendImpl("  end;")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areExactly(1, ruleKeyAtLine("RedundantBooleanRule", builder.getOffset() + 5));
  }

  @Test
  void testRedundantNestedBooleanComparisonShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure MyProcedure;")
            .appendImpl("var")
            .appendImpl("  X: Boolean;")
            .appendImpl("begin")
            .appendImpl("  if ((((X))) = (((True)))) then begin")
            .appendImpl("    DoSomething;")
            .appendImpl("  end;")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areExactly(1, ruleKeyAtLine("RedundantBooleanRule", builder.getOffset() + 5));
  }

  @Test
  void testNeedlesslyInvertedBooleanShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendDecl("procedure Foo(Bar: Boolean);")
            .appendImpl("procedure Baz;")
            .appendImpl("begin")
            .appendImpl("  Foo(not True);")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areExactly(1, ruleKeyAtLine("RedundantBooleanRule", builder.getOffset() + 3));
  }
}