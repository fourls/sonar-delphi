package au.com.integradev.delphi.antlr.ast.node;

import au.com.integradev.delphi.antlr.DelphiLexer;
import au.com.integradev.delphi.type.Type;
import au.com.integradev.delphi.type.Typed;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface FormalParameterNode extends DelphiNode, Typed {
  List<FormalParameterData> getParameters();

  @Nullable
  TypeNode getTypeNode();

  default ExpressionNode getDefaultValue() {
    return getFirstChildOfType(ExpressionNode.class);
  }

  default boolean isOut() {
    return getFirstChildWithId(DelphiLexer.OUT) != null;
  }

  default boolean isVar() {
    return getFirstChildWithId(DelphiLexer.VAR) != null;
  }

  default boolean isConst() {
    return getFirstChildWithId(DelphiLexer.CONST) != null;
  }

  interface FormalParameterData extends Typed {
    NameDeclarationNode getNode();

    @Override
    @Nonnull
    Type getType();

    String getImage();

    boolean hasDefaultValue();

    ExpressionNode getDefaultValue();

    boolean isOut();

    boolean isVar();

    boolean isConst();
  }
}
