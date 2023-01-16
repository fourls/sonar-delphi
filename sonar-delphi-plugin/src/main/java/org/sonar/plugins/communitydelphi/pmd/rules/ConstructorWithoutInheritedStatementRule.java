/*
 * Sonar Delphi Plugin
 * Copyright (C) 2015 Fabricio Colombo
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

import net.sourceforge.pmd.RuleContext;
import org.sonar.plugins.communitydelphi.antlr.ast.node.MethodImplementationNode;
import org.sonar.plugins.communitydelphi.symbol.declaration.TypeNameDeclaration;

public class ConstructorWithoutInheritedStatementRule extends NoInheritedStatementRule {

  @Override
  public RuleContext visit(MethodImplementationNode method, RuleContext data) {
    if (method.isConstructor() && !isExcluded(method)) {
      checkViolation(method, data);
    }
    return super.visit(method, data);
  }

  private static boolean isExcluded(MethodImplementationNode method) {
    TypeNameDeclaration declaration = method.getTypeDeclaration();
    return method.isClassMethod() || (declaration != null && declaration.getType().isRecord());
  }
}