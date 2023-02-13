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
package au.com.integradev.delphi.pmd.rules;

import au.com.integradev.delphi.antlr.ast.node.ArgumentListNode;
import au.com.integradev.delphi.antlr.ast.node.DelphiNode;
import au.com.integradev.delphi.antlr.ast.node.MethodParametersNode;
import au.com.integradev.delphi.antlr.ast.node.NameReferenceNode;
import au.com.integradev.delphi.antlr.ast.node.PrimaryExpressionNode;
import au.com.integradev.delphi.symbol.declaration.MethodNameDeclaration;
import au.com.integradev.delphi.type.Type;
import au.com.integradev.delphi.type.Typed;
import net.sourceforge.pmd.RuleContext;
import au.com.integradev.delphi.antlr.ast.node.Node;

public class EmptyBracketsRule extends AbstractDelphiRule {
  private static final String SYSTEM_ASSIGNED_IMAGE = "System.Assigned";

  @Override
  public RuleContext visit(MethodParametersNode parameters, RuleContext data) {
    if (parameters.isEmpty()) {
      addViolation(data, parameters);
    }
    return super.visit(parameters, data);
  }

  @Override
  public RuleContext visit(ArgumentListNode arguments, RuleContext data) {
    if (arguments.isEmpty()
        && !isExplicitArrayConstructorInvocation(arguments)
        && !isRequiredToDistinguishProceduralFromReturn(arguments)) {
      addViolation(data, arguments);
    }
    return super.visit(arguments, data);
  }

  private static boolean isExplicitArrayConstructorInvocation(ArgumentListNode arguments) {
    Node previous = arguments.jjtGetParent().jjtGetChild(arguments.jjtGetChildIndex() - 1);
    return previous instanceof NameReferenceNode
        && ((NameReferenceNode) previous).isExplicitArrayConstructorInvocation();
  }

  private static boolean isRequiredToDistinguishProceduralFromReturn(ArgumentListNode arguments) {
    return isProcVarInvocation(arguments) || isPartOfSystemAssignedArgumentExpression(arguments);
  }

  private static boolean isProcVarInvocation(ArgumentListNode arguments) {
    Node previous = arguments.jjtGetParent().jjtGetChild(arguments.jjtGetChildIndex() - 1);
    if (previous instanceof Typed) {
      Type type = ((Typed) previous).getType();
      return type.isProcedural() && !type.isMethod();
    }
    return true;
  }

  private static boolean isPartOfSystemAssignedArgumentExpression(ArgumentListNode arguments) {
    DelphiNode parent = arguments.jjtGetParent();
    if (parent instanceof PrimaryExpressionNode) {
      DelphiNode grandparent = parent.jjtGetParent();
      if (grandparent instanceof ArgumentListNode) {
        DelphiNode prev =
            grandparent.jjtGetParent().jjtGetChild(grandparent.jjtGetChildIndex() - 1);
        if (prev instanceof NameReferenceNode) {
          var declaration = ((NameReferenceNode) prev).getLastName().getNameDeclaration();
          return declaration instanceof MethodNameDeclaration
              && ((MethodNameDeclaration) declaration)
                  .fullyQualifiedName()
                  .equals(SYSTEM_ASSIGNED_IMAGE);
        }
      }
    }
    return false;
  }
}
