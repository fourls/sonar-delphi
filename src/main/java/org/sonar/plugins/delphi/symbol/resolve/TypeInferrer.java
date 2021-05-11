package org.sonar.plugins.delphi.symbol.resolve;

import java.util.Comparator;
import java.util.Set;
import net.sourceforge.pmd.lang.ast.Node;
import org.sonar.plugins.delphi.antlr.ast.node.ArrayConstructorNode;
import org.sonar.plugins.delphi.antlr.ast.node.ExpressionNode;
import org.sonar.plugins.delphi.type.ArrayOption;
import org.sonar.plugins.delphi.type.DelphiType;
import org.sonar.plugins.delphi.type.Type;
import org.sonar.plugins.delphi.type.Typed;
import org.sonar.plugins.delphi.type.factory.TypeFactory;
import org.sonar.plugins.delphi.type.intrinsic.IntrinsicType;

public final class TypeInferrer {
  private final TypeFactory typeFactory;

  public TypeInferrer(TypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  public Type infer(Typed typed) {
    Type type = typed == null ? DelphiType.unknownType() : typed.getType();
    if (typed instanceof ExpressionNode) {
      ExpressionNode expression = (ExpressionNode) typed;
      Node arrayConstructor = expression.skipParentheses().jjtGetChild(0);

      if (arrayConstructor instanceof ArrayConstructorNode) {
        type = inferArrayConstructor((ArrayConstructorNode) arrayConstructor);
      } else if (expression.isIntegerLiteral() && type.size() <= 4) {
        type = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
      }
    }
    return type;
  }

  private Type inferArrayConstructor(ArrayConstructorNode arrayConstructor) {
    Type element =
        arrayConstructor.getElements().stream()
            .map(this::infer)
            .max(Comparator.comparingInt(Type::size))
            .orElse(DelphiType.voidType());

    return typeFactory.array(null, element, Set.of(ArrayOption.DYNAMIC));
  }
}