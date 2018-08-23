package org.sonar.plugins.delphi.pmd.rules;

import net.sourceforge.pmd.RuleContext;
import org.sonar.plugins.delphi.antlr.DelphiLexer;
import org.sonar.plugins.delphi.antlr.ast.DelphiPMDNode;

public class ReRaiseExceptionRule extends DelphiRule {

  /**
   * This rule looks for exception blocks where the exception is raised a second time at the end of
   * the except block. This is done by searching for any begin block (except blocks always contain
   * begin/end blocks) and any children which are 'raise' statements. If the next node after the
   * raise statement is not a semicolon, it is assumed this is a re-raised exception and a violation
   * is raised.
   *
   * @param node the current node
   * @param ctx the ruleContext to store the violations
   */
  @Override
  public void visit(DelphiPMDNode node, RuleContext ctx) {

    if (node.getType() == DelphiLexer.BEGIN) {
      // Exception blocks always contain 'begin' statements, so look at the children of begin blocks
      // Note: Would be good to just access the next node after a 'raise' but list isn't accessible here

      for (int i = 0; i < node.getChildCount() - 1; i++) {
        DelphiPMDNode childNode = (DelphiPMDNode) node.getChild(i);

        if (childNode != null) {
          if (childNode.getType() == DelphiLexer.RAISE) {
            DelphiPMDNode exceptionDeclarationNode = (DelphiPMDNode) node.getChild(i + 1);
            if (exceptionDeclarationNode != null) {
              if (!exceptionDeclarationNode.getText().equals(";")) {
                addViolation(ctx, childNode);
              }
            }
          }
        }
      }
    }


  }

}