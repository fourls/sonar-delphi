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

import au.com.integradev.delphi.antlr.DelphiLexer;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.TypeNameDeclaration;
import org.sonar.plugins.communitydelphi.api.ast.ArgumentListNode;
import org.sonar.plugins.communitydelphi.api.ast.BinaryExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.ExpressionNode;
import org.sonar.plugins.communitydelphi.api.ast.NameReferenceNode;
import au.com.integradev.delphi.operator.BinaryOperator;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.NameDeclaration;
import org.sonar.plugins.communitydelphi.api.type.Type;
import org.sonar.plugins.communitydelphi.api.type.Type.ClassReferenceType;
import org.sonar.plugins.communitydelphi.api.type.Type.ProceduralType;
import au.com.integradev.delphi.type.intrinsic.IntrinsicType;
import java.util.List;
import net.sourceforge.pmd.RuleContext;
import org.sonar.plugins.communitydelphi.api.ast.Node;

public abstract class AbstractCastRule extends AbstractDelphiRule {
  protected abstract boolean isViolation(Type originalType, Type castType);

  @Override
  public RuleContext visit(BinaryExpressionNode binaryExpression, RuleContext data) {
    if (binaryExpression.getOperator() == BinaryOperator.AS) {
      Type originalType = getOriginalType(binaryExpression.getLeft());
      Type castedType = getSoftCastedType(binaryExpression.getRight());

      if (isViolation(originalType, castedType)
          && !originalType.isUnknown()
          && !castedType.isUnknown()) {
        addViolation(data, binaryExpression);
      }
    }
    return super.visit(binaryExpression, data);
  }

  @Override
  public RuleContext visit(ArgumentListNode argumentList, RuleContext data) {
    List<ExpressionNode> arguments = argumentList.getArguments();
    if (arguments.size() == 1) {
      Type originalType = getOriginalType(arguments.get(0));
      Type castedType = getHardCastedType(argumentList);
      if (castedType != null && isViolation(originalType, castedType)) {
        addViolation(data, argumentList);
      }
    }
    return super.visit(argumentList, data);
  }

  private static Type getOriginalType(ExpressionNode expression) {
    Type result = expression.getType();
    if (result.isMethod()) {
      result = ((ProceduralType) result).returnType();
    }
    return result;
  }

  private static Type getSoftCastedType(ExpressionNode expression) {
    Type result = expression.getType();
    if (result.isClassReference()) {
      result = ((ClassReferenceType) result).classType();
    }
    return result;
  }

  private static Type getHardCastedType(ArgumentListNode argumentList) {
    Node previous = argumentList.jjtGetParent().jjtGetChild(argumentList.jjtGetChildIndex() - 1);
    if (previous instanceof NameReferenceNode) {
      NameReferenceNode nameReference = ((NameReferenceNode) previous);
      NameDeclaration declaration = nameReference.getLastName().getNameDeclaration();
      if (declaration instanceof TypeNameDeclaration) {
        return ((TypeNameDeclaration) declaration).getType();
      }
    }

    switch (previous.jjtGetId()) {
      case DelphiLexer.STRING:
        return argumentList.getTypeFactory().getIntrinsic(IntrinsicType.STRING);
      case DelphiLexer.FILE:
        return argumentList.getTypeFactory().untypedFile();
      default:
        return null;
    }
  }
}
