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
package au.com.integradev.delphi.symbol.declaration;

import au.com.integradev.delphi.antlr.ast.node.GenericDefinitionNode.TypeParameter;
import au.com.integradev.delphi.antlr.ast.node.NameDeclarationNode;
import au.com.integradev.delphi.antlr.ast.node.TypeAliasNode;
import au.com.integradev.delphi.antlr.ast.node.TypeDeclarationNode;
import au.com.integradev.delphi.antlr.ast.node.TypeNode;
import au.com.integradev.delphi.antlr.ast.node.TypeReferenceNode;
import au.com.integradev.delphi.symbol.NameDeclaration;
import au.com.integradev.delphi.symbol.SymbolicNode;
import au.com.integradev.delphi.type.Type;
import au.com.integradev.delphi.type.generic.TypeSpecializationContext;
import com.google.common.collect.ComparisonChain;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TypeNameDeclaration extends AbstractNameDeclaration
    implements GenerifiableDeclaration, TypedDeclaration {
  private final String fullyQualifiedName;
  private final Type type;
  private final List<TypedDeclaration> typeParameters;
  private final TypeNameDeclaration aliased;

  public TypeNameDeclaration(TypeDeclarationNode node) {
    this(
        new SymbolicNode(node.getTypeNameNode().getIdentifier(), node.getScope()),
        node.getType(),
        node.fullyQualifiedName(),
        extractTypeParameters(node),
        extractAliasedTypeDeclaration(node));
  }

  public TypeNameDeclaration(SymbolicNode node, Type type, String fullyQualifiedName) {
    this(node, type, fullyQualifiedName, Collections.emptyList(), null);
  }

  public TypeNameDeclaration(
      SymbolicNode node,
      Type type,
      String fullyQualifiedName,
      List<TypedDeclaration> typeParameters) {
    this(node, type, fullyQualifiedName, typeParameters, null);
  }

  private TypeNameDeclaration(
      SymbolicNode node,
      Type type,
      String fullyQualifiedName,
      List<TypedDeclaration> typeParameters,
      @Nullable TypeNameDeclaration aliased) {
    super(node);
    this.type = type;
    this.fullyQualifiedName = fullyQualifiedName;
    this.typeParameters = typeParameters;
    this.aliased = aliased;
  }

  private static List<TypedDeclaration> extractTypeParameters(TypeDeclarationNode node) {
    return node.getTypeNameNode().getTypeParameters().stream()
        .map(TypeParameter::getLocation)
        .map(NameDeclarationNode::getNameDeclaration)
        .map(TypedDeclaration.class::cast)
        .collect(Collectors.toUnmodifiableList());
  }

  private static TypeNameDeclaration extractAliasedTypeDeclaration(TypeDeclarationNode node) {
    TypeNode typeNode = node.getTypeNode();
    if (typeNode instanceof TypeAliasNode) {
      TypeReferenceNode original = ((TypeAliasNode) typeNode).getAliasedTypeNode();
      NameDeclaration originalDeclaration = original.getTypeDeclaration();
      if (originalDeclaration instanceof TypeNameDeclaration) {
        return (TypeNameDeclaration) originalDeclaration;
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public Type getType() {
    return type;
  }

  public String fullyQualifiedName() {
    return fullyQualifiedName;
  }

  @Override
  public List<TypedDeclaration> getTypeParameters() {
    return typeParameters;
  }

  @Nullable
  public TypeNameDeclaration getAliased() {
    return aliased;
  }

  @Override
  protected NameDeclaration doSpecialization(TypeSpecializationContext context) {
    return new TypeNameDeclaration(
        node,
        type.specialize(context),
        fullyQualifiedName,
        typeParameters.stream()
            .map(parameter -> parameter.specialize(context))
            .map(TypedDeclaration.class::cast)
            .collect(Collectors.toUnmodifiableList()));
  }

  @Override
  public boolean equals(Object other) {
    if (super.equals(other)) {
      TypeNameDeclaration that = (TypeNameDeclaration) other;
      return getImage().equalsIgnoreCase(that.getImage())
          && type.is(that.type)
          && typeParameters.equals(that.typeParameters);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getImage().toLowerCase(), type.getImage().toLowerCase(), getTypeParameters());
  }

  @Override
  public int compareTo(@Nonnull NameDeclaration other) {
    int result = super.compareTo(other);

    if (result == 0) {
      TypeNameDeclaration that = (TypeNameDeclaration) other;
      result =
          ComparisonChain.start()
              .compare(type.getImage(), that.type.getImage(), String.CASE_INSENSITIVE_ORDER)
              .compare(typeParameters.size(), that.typeParameters.size())
              .result();
    }

    return result;
  }

  @Override
  public String toString() {
    return "type " + getType().getImage();
  }
}
