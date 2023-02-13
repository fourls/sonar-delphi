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
package au.com.integradev.delphi.antlr.ast.node;

import au.com.integradev.delphi.antlr.ast.visitors.DelphiParserVisitor;
import au.com.integradev.delphi.type.Type;
import org.antlr.runtime.Token;
import org.jetbrains.annotations.NotNull;

public final class FieldDeclarationNodeImpl extends AbstractDelphiNode
    implements FieldDeclarationNode {
  public FieldDeclarationNodeImpl(Token token) {
    super(token);
  }

  public FieldDeclarationNodeImpl(int tokenType) {
    super(tokenType);
  }

  @Override
  public <T> T accept(DelphiParserVisitor<T> visitor, T data) {
    return visitor.visit(this, data);
  }

  @Override
  public VisibilityType getVisibility() {
    DelphiNode parent = jjtGetParent();
    if (parent instanceof FieldSectionNode) {
      return ((FieldSectionNode) parent).getVisibility();
    }
    return VisibilityType.PUBLIC;
  }

  @Override
  public NameDeclarationListNode getDeclarationList() {
    return (NameDeclarationListNode) jjtGetChild(0);
  }

  @Override
  public TypeNode getTypeNode() {
    return (TypeNode) jjtGetChild(1);
  }

  @Override
  @NotNull
  public Type getType() {
    return getTypeNode().getType();
  }
}
