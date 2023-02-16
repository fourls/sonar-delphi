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

import org.sonar.plugins.communitydelphi.api.symbol.declaration.TypeNameDeclaration;
import org.sonar.plugins.communitydelphi.api.ast.TypeDeclarationNode;
import org.sonar.plugins.communitydelphi.api.ast.TypeNode;
import org.sonar.plugins.communitydelphi.api.ast.VisibilitySectionNode;
import net.sourceforge.pmd.RuleContext;

public class EmptyInterfaceRule extends AbstractDelphiRule {

  @Override
  public RuleContext visit(TypeDeclarationNode typeDecl, RuleContext data) {
    if (typeDecl.isInterface()) {
      TypeNode typeNode = typeDecl.getTypeNode();
      boolean isEmpty = typeNode.getFirstChildOfType(VisibilitySectionNode.class) == null;
      TypeNameDeclaration declaration = typeDecl.getTypeNameDeclaration();

      if (isEmpty && (declaration == null || !declaration.isForwardDeclaration())) {
        addViolation(data, typeDecl.getTypeNameNode());
      }
    }

    return super.visit(typeDecl, data);
  }
}
