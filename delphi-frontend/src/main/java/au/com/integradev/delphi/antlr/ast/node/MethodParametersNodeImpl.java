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

import au.com.integradev.delphi.antlr.ast.node.FormalParameterNode.FormalParameterData;
import au.com.integradev.delphi.antlr.ast.visitors.DelphiParserVisitor;
import au.com.integradev.delphi.type.Type;
import java.util.Collections;
import java.util.List;
import org.antlr.runtime.Token;

public final class MethodParametersNodeImpl extends AbstractDelphiNode
    implements MethodParametersNode {
  private String image;
  private FormalParameterListNode parameterList;

  public MethodParametersNodeImpl(Token token) {
    super(token);
  }

  public MethodParametersNodeImpl(int tokenType) {
    super(tokenType);
  }

  @Override
  public <T> T accept(DelphiParserVisitor<T> visitor, T data) {
    return visitor.visit(this, data);
  }

  @Override
  public List<FormalParameterData> getParameters() {
    return isEmpty() ? Collections.emptyList() : getFormalParametersList().getParameters();
  }

  @Override
  public List<Type> getParameterTypes() {
    return isEmpty() ? Collections.emptyList() : getFormalParametersList().getParameterTypes();
  }

  @Override
  public String getImage() {
    if (image == null) {
      image = "(" + (isEmpty() ? "" : getFormalParametersList().getImage()) + ")";
    }
    return image;
  }

  @Override
  public FormalParameterListNode getFormalParametersList() {
    if (parameterList == null && !isEmpty()) {
      parameterList = (FormalParameterListNode) jjtGetChild(1);
    }
    return parameterList;
  }

  @Override
  public boolean isEmpty() {
    return jjtGetNumChildren() < 3;
  }
}
