package org.sonar.plugins.delphi.symbol;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.sonar.plugins.delphi.antlr.ast.node.QualifiedNameDeclarationNode;
import org.sonar.plugins.delphi.antlr.ast.node.TypeDeclarationNode;
import org.sonar.plugins.delphi.type.Type;
import org.sonar.plugins.delphi.type.Typed;

public final class TypeNameDeclaration extends DelphiNameDeclaration implements Typed {
  private final String image;
  private boolean isForwardDeclaration;
  private Type type;

  public TypeNameDeclaration(TypeDeclarationNode typeNode) {
    super(typeNode.getTypeNameNode(), typeNode.getScope());
    this.image = typeNode.simpleName();
    this.type = typeNode.getType();
  }

  @Override
  public QualifiedNameDeclarationNode getNode() {
    return (QualifiedNameDeclarationNode) super.getNode();
  }

  @Override
  @NotNull
  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return "type " + getType().getImage();
  }

  void setIsForwardDeclaration(Type fullType) {
    isForwardDeclaration = true;
    this.type = fullType;
  }

  public boolean isForwardDeclaration() {
    return isForwardDeclaration;
  }

  @Override
  public String getImage() {
    return image;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    TypeNameDeclaration that = (TypeNameDeclaration) other;
    return getImage().equalsIgnoreCase(that.getImage())
        && isForwardDeclaration == that.isForwardDeclaration;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getImage().toLowerCase(), isForwardDeclaration);
  }
}