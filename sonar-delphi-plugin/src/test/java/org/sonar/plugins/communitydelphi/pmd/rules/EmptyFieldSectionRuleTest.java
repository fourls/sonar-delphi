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

class EmptyFieldSectionRuleTest extends BasePmdRuleTest {

  @Test
  void testRegularFieldSectionShouldNotAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendDecl("type")
            .appendDecl("  TClass = class(TObject)")
            .appendDecl("  var")
            .appendDecl("    FObject: TObject;")
            .appendDecl("  end;");

    execute(builder);

    assertIssues().areNot(ruleKey("EmptyFieldSectionRule"));
  }

  @Test
  void testEmptyFieldSectionShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendDecl("type")
            .appendDecl("  TClass = class(TObject)")
            .appendDecl("  var")
            .appendDecl("  end;");

    execute(builder);

    assertIssues()
        .areExactly(1, ruleKeyAtLine("EmptyFieldSectionRule", builder.getOffsetDecl() + 3));
  }

  @Test
  void testEmptyClassVarFieldSectionShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendDecl("type")
            .appendDecl("  TClass = class(TObject)")
            .appendDecl("  class var")
            .appendDecl("  end;");

    execute(builder);

    assertIssues()
        .areExactly(1, ruleKeyAtLine("EmptyFieldSectionRule", builder.getOffsetDecl() + 3));
  }
}