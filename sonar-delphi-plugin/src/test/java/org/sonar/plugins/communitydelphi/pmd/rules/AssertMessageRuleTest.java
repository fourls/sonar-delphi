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

class AssertMessageRuleTest extends BasePmdRuleTest {
  @Test
  void testAssertWithoutErrorMessageShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure Foo;")
            .appendImpl("begin")
            .appendImpl("  Assert(False);")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areExactly(1, ruleKeyAtLine("AssertMessageRule", builder.getOffset() + 3));
  }

  @Test
  void testAssertWithErrorMessageShouldNotAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure Foo;")
            .appendImpl("begin")
            .appendImpl("  Assert(False, 'This always fails.');")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areNot(ruleKey("AssertMessageRule"));
  }

  @Test
  void testAssertMethodReferenceShouldNotAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure Foo;")
            .appendImpl("var")
            .appendImpl("  Proc: reference to procedure(Expr: Boolean; Message: String);")
            .appendImpl("begin")
            .appendImpl("  Proc := System.Assert;")
            .appendImpl("end;");

    execute(builder);

    assertIssues().areNot(ruleKey("AssertMessageRule"));
  }
}