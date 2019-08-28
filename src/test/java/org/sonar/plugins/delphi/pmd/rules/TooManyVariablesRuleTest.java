package org.sonar.plugins.delphi.pmd.rules;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.empty;
import static org.sonar.plugins.delphi.utils.matchers.IssueMatchers.hasRuleKeyAtLine;

import org.junit.Test;
import org.sonar.plugins.delphi.utils.builders.DelphiTestUnitBuilder;

public class TooManyVariablesRuleTest extends BasePmdRuleTest {

  @Test
  public void testOneVariableShouldNotAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure Foo;")
            .appendImpl("var")
            .appendImpl("  MyVar: Boolean;")
            .appendImpl("begin")
            .appendImpl("  MyVar := True;")
            .appendImpl("end;");

    execute(builder);

    assertIssues(empty());
  }

  @Test
  public void testTooManyVariablesShouldAddIssue() {
    DelphiTestUnitBuilder builder =
        new DelphiTestUnitBuilder()
            .appendImpl("procedure Foo;")
            .appendImpl("var")
            .appendImpl("  MyVar1: Boolean;")
            .appendImpl("  MyVar2: Boolean;")
            .appendImpl("  MyVar3: Boolean;")
            .appendImpl("  MyVar4: Boolean;")
            .appendImpl("  MyVar5: Boolean;")
            .appendImpl("  MyVar6: Boolean;")
            .appendImpl("  MyVar7: Boolean;")
            .appendImpl("  MyVar8: Boolean;")
            .appendImpl("  MyVar9: Boolean;")
            .appendImpl("  MyVar10: Boolean;")
            .appendImpl("  MyVar11: Boolean;")
            .appendImpl("begin")
            .appendImpl("  MyVar1 := MyVar2;")
            .appendImpl("end;");

    execute(builder);

    assertIssues(hasItem(hasRuleKeyAtLine("TooManyVariablesRule", builder.getOffSet() + 1)));
  }
}