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

class NoSemiAfterMethodDeclarationRuleTest extends BasePmdRuleTest {

  @Test
  void testMethodDeclarationsWithSemicolonsShouldNotAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendDecl("type")
            .appendDecl("  TType = class(TObject)")
            .appendDecl("  public")
            .appendDecl("    constructor Create; override;")
            .appendDecl("    destructor Destroy; override;")
            .appendDecl("    procedure MyProcedure; overload;")
            .appendDecl("    function MyFunction: String; overload;")
            .appendDecl("  end;");

    execute(builder);

    assertIssues().areNot(ruleKey("NoSemiAfterMethodDeclarationRule"));
  }

  @Test
  void testMethodDeclarationsWithoutSemicolonsShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendDecl("type")
            .appendDecl("  TType = class(TObject)")
            .appendDecl("  public")
            .appendDecl("    constructor Create; override")
            .appendDecl("    destructor Destroy; override")
            .appendDecl("    procedure MyProcedure; overload")
            .appendDecl("    function MyFunction: String; overload")
            .appendDecl("  end;");

    execute(builder);

    assertIssues()
        .areExactly(
            1, ruleKeyAtLine("NoSemiAfterMethodDeclarationRule", builder.getOffsetDecl() + 4))
        .areExactly(
            1, ruleKeyAtLine("NoSemiAfterMethodDeclarationRule", builder.getOffsetDecl() + 5))
        .areExactly(
            1, ruleKeyAtLine("NoSemiAfterMethodDeclarationRule", builder.getOffsetDecl() + 6))
        .areExactly(
            1, ruleKeyAtLine("NoSemiAfterMethodDeclarationRule", builder.getOffsetDecl() + 7));
  }
}