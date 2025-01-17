/*
 * Sonar Delphi Plugin
 * Copyright (C) 2019 Integrated Application Development
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
package au.com.integradev.delphi.operator;

import static au.com.integradev.delphi.type.intrinsic.IntrinsicArgumentMatcher.ANY_ORDINAL;
import static au.com.integradev.delphi.type.intrinsic.IntrinsicArgumentMatcher.ANY_SET;
import static au.com.integradev.delphi.type.intrinsic.IntrinsicArgumentMatcher.POINTER_MATH_OPERAND;
import static org.sonar.plugins.communitydelphi.api.type.TypeFactory.untypedType;

import au.com.integradev.delphi.type.factory.TypeFactoryImpl;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.communitydelphi.api.operator.BinaryOperator;
import org.sonar.plugins.communitydelphi.api.operator.Operator;
import org.sonar.plugins.communitydelphi.api.operator.UnaryOperator;
import org.sonar.plugins.communitydelphi.api.symbol.Invocable;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.RoutineKind;
import org.sonar.plugins.communitydelphi.api.type.IntrinsicType;
import org.sonar.plugins.communitydelphi.api.type.Type;
import org.sonar.plugins.communitydelphi.api.type.Type.AliasType;
import org.sonar.plugins.communitydelphi.api.type.Type.ArrayConstructorType;
import org.sonar.plugins.communitydelphi.api.type.Type.CollectionType;
import org.sonar.plugins.communitydelphi.api.type.Type.IntegerType;
import org.sonar.plugins.communitydelphi.api.type.Type.PointerType;
import org.sonar.plugins.communitydelphi.api.type.Type.ProceduralType;
import org.sonar.plugins.communitydelphi.api.type.Type.StructType;
import org.sonar.plugins.communitydelphi.api.type.TypeFactory;

public class OperatorInvocableCollector {
  private final TypeFactory typeFactory;
  private List<Type> operands;

  public OperatorInvocableCollector(TypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  public Set<Invocable> collect(BinaryOperator operator, Type left, Type right) {
    left = invokeProcedural(left);
    right = invokeProcedural(right);
    operands = List.of(left, right);

    Set<Invocable> result = collectBinary(left, operator);
    result.addAll(collectBinary(right, operator));

    return result;
  }

  public Set<Invocable> collect(UnaryOperator operator, Type operand) {
    operand = invokeProcedural(operand);
    operands = List.of(operand);

    return collectUnary(operand, operator);
  }

  private static Type invokeProcedural(Type type) {
    if (type instanceof ProceduralType) {
      return ((ProceduralType) type).returnType();
    }
    return type;
  }

  private Set<Invocable> collectBinary(Type type, BinaryOperator operator) {
    Set<Invocable> result = new HashSet<>();

    if (type.isStruct()) {
      result.addAll(collectOperatorOverloads((StructType) type, operator));
    } else if (type.isPointer()) {
      result.addAll(createPointerMath((PointerType) type, operator));
    } else if (type.isVariant() && operator != BinaryOperator.IN && operator != BinaryOperator.AS) {
      result.add(createVariantBinary(operator));
    } else if (type.isSet() || type.isArrayConstructor()) {
      result.addAll(createSetLike(type, operator));
    } else if (type.isDynamicArray()) {
      result.addAll(createDynamicArray((CollectionType) type, operator));
    } else if (type.isInteger()) {
      result.addAll(createIntegerBinary(operator));
    } else if (type.isReal()) {
      result.addAll(createRealBinary(operator));
    } else if (type.isBoolean()) {
      result.addAll(createBooleanBinary(operator));
    } else if (type.isString() || type.isChar()) {
      result.addAll(createString(operator));
    }

    switch (operator) {
      case EQUAL:
        result.add(createComparison("Equal"));
        break;
      case GREATER_THAN:
        result.add(createComparison("GreaterThan"));
        break;
      case LESS_THAN:
        result.add(createComparison("LessThan"));
        break;
      case GREATER_THAN_EQUAL:
        result.add(createComparison("GreaterThanEqual"));
        break;
      case LESS_THAN_EQUAL:
        result.add(createComparison("LessThanEqual"));
        break;
      case NOT_EQUAL:
        result.add(createComparison("NotEqual"));
        break;
      case IN:
        result.add(createIn());
        break;
      default:
        // do nothing
    }

    return result;
  }

  private static Set<Invocable> addAll(Set<Invocable> collection, Collection<Invocable> other) {
    collection.addAll(other);
    return collection;
  }

  private static Set<Invocable> addAll(Set<Invocable> collection, Invocable... other) {
    return addAll(collection, Set.of(other));
  }

  private static Set<Invocable> collectOperatorOverloads(StructType type, Operator operator) {
    return type.typeScope().getRoutineDeclarations().stream()
        .filter(method -> method.getRoutineKind() == RoutineKind.OPERATOR)
        .filter(method -> operator.getNames().contains(method.getImage()))
        .collect(Collectors.toSet());
  }

  private Set<Invocable> createPointerMath(PointerType type, BinaryOperator operator) {
    if (!type.allowsPointerMath()) {
      return Sets.newHashSet();
    }

    switch (operator) {
      case ADD:
        return createPointerMathAdd(type);
      case SUBTRACT:
        return createPointerMathSubtract(type);
      default:
        return Sets.newHashSet();
    }
  }

  private Invocable createVariantBinary(BinaryOperator operator) {
    final String PREFIX = "Variant::";
    Type variant = typeFactory.getIntrinsic(IntrinsicType.VARIANT);
    List<Type> arguments = List.of(variant, variant);
    Type returnType;
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
      case LESS_THAN:
      case LESS_THAN_EQUAL:
      case GREATER_THAN:
      case GREATER_THAN_EQUAL:
        returnType = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
        break;
      default:
        returnType = variant;
    }
    return new OperatorIntrinsic(PREFIX + operator.name(), arguments, returnType);
  }

  private Set<Invocable> createPointerMathAdd(PointerType type) {
    final String NAME = "Add";
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);

    return Sets.newHashSet(
        new OperatorIntrinsic(NAME, List.of(type, integer), type),
        new OperatorIntrinsic(NAME, List.of(integer, type), type),
        new OperatorIntrinsic(NAME, List.of(type, POINTER_MATH_OPERAND), type));
  }

  private Set<Invocable> createPointerMathSubtract(PointerType type) {
    final String NAME = "Subtract";
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);

    return Sets.newHashSet(
        new OperatorIntrinsic(NAME, List.of(type, integer), type),
        new OperatorIntrinsic(NAME, List.of(type, POINTER_MATH_OPERAND), integer));
  }

  private Set<Invocable> createSetLike(Type type, BinaryOperator operator) {
    String name;

    switch (operator) {
      case ADD:
        name = "Add";
        break;
      case SUBTRACT:
        name = "Subtract";
        break;
      case MULTIPLY:
        name = "Multiply";
        break;
      default:
        name = null;
    }

    Set<Invocable> result = new HashSet<>();

    if (name != null) {
      if (type.isArrayConstructor()) {
        type = normalizeArrayConstructor((ArrayConstructorType) type);
      }
      result.add(new OperatorIntrinsic(name, List.of(type, type), type));
    }

    return result;
  }

  private Type normalizeArrayConstructor(ArrayConstructorType type) {
    Type elementType =
        type.elementTypes().stream()
            .max(Comparator.comparingInt(Type::size))
            .orElse(TypeFactory.voidType());
    return typeFactory.arrayConstructor(List.of(elementType));
  }

  private static Set<Invocable> createDynamicArray(CollectionType type, Operator operator) {
    Set<Invocable> result = new HashSet<>();
    if (operator == BinaryOperator.ADD) {
      result.add(new OperatorIntrinsic("Add", List.of(type, type), type));
    }
    return result;
  }

  private Set<Invocable> createIntegerBinary(BinaryOperator operator) {
    switch (operator) {
      case AND:
        return createBitwiseAnd();
      case OR:
        return createBitwiseOr("Or");
      case XOR:
        return createBitwiseOr("Xor");
      case ADD:
        return createIntegerArithmeticBinary("Add");
      case SUBTRACT:
        return createIntegerArithmeticBinary("Subtract");
      case MULTIPLY:
        return createIntegerArithmeticBinary("Multiply");
      case DIVIDE:
        return createDivide();
      case DIV:
        return createIntegerArithmeticBinary("IntDivide");
      case MOD:
        return createIntegerArithmeticBinary("Modulus");
      case SHL:
        return createShift("Left");
      case SHR:
        return createShift("Right");
      default:
        return Sets.newHashSet();
    }
  }

  private Set<Invocable> createRealBinary(BinaryOperator operator) {
    switch (operator) {
      case ADD:
        return createRealArithmeticBinary("Add");
      case SUBTRACT:
        return createRealArithmeticBinary("Subtract");
      case MULTIPLY:
        return createRealArithmeticBinary("Multiply");
      case DIVIDE:
        return createDivide();
      default:
        return Sets.newHashSet();
    }
  }

  private Set<Invocable> createBooleanBinary(BinaryOperator operator) {
    switch (operator) {
      case AND:
        return createLogical("And");
      case OR:
        return createLogical("Or");
      case XOR:
        return createLogical("Xor");
      default:
        return Sets.newHashSet();
    }
  }

  private Set<Invocable> createString(BinaryOperator operator) {
    Set<Invocable> result = Sets.newHashSet();
    if (operator == BinaryOperator.ADD) {
      Type string = typeFactory.getIntrinsic(IntrinsicType.STRING);
      result.add(new OperatorIntrinsic("Add", List.of(string, string), string));
    }
    return result;
  }

  private Set<Invocable> createBitwiseAnd() {
    final String NAME = "BitwiseAnd";

    Set<Invocable> result = createNativeIntegerBinary(NAME);
    if (!result.isEmpty()) {
      return result;
    }

    Type int8 = typeFactory.getIntrinsic(IntrinsicType.SHORTINT);
    Type int16 = typeFactory.getIntrinsic(IntrinsicType.SMALLINT);
    Type int32 = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);

    Type uint8 = typeFactory.getIntrinsic(IntrinsicType.BYTE);
    Type uint16 = typeFactory.getIntrinsic(IntrinsicType.WORD);
    Type uint32 = typeFactory.getIntrinsic(IntrinsicType.CARDINAL);
    Type uint64 = typeFactory.getIntrinsic(IntrinsicType.UINT64);

    Type uint15 = ((TypeFactoryImpl) typeFactory).anonymousUInt15();
    Type uint31 = ((TypeFactoryImpl) typeFactory).anonymousUInt31();

    addAll(
        result,
        new OperatorIntrinsic(NAME, List.of(int8, int8), int8),
        new OperatorIntrinsic(NAME, List.of(int16, int16), int16),
        new OperatorIntrinsic(NAME, List.of(uint8, uint8), uint8),
        new OperatorIntrinsic(NAME, List.of(uint15, uint15), uint15),
        new OperatorIntrinsic(NAME, List.of(uint16, uint16), uint16),
        new OperatorIntrinsic(NAME, List.of(uint31, uint31), uint31));
    addAll(result, createWithInterleavedTypes(NAME, int32, uint32));
    addAll(result, createWithInterleavedTypes(NAME, int64, uint64));

    return result;
  }

  private Set<Invocable> createBitwiseOr(String suffix) {
    String name = "Bitwise" + suffix;

    Set<Invocable> result = createNativeIntegerBinary(name);
    if (!result.isEmpty()) {
      return result;
    }

    Type int8 = typeFactory.getIntrinsic(IntrinsicType.SHORTINT);
    Type int16 = typeFactory.getIntrinsic(IntrinsicType.SMALLINT);
    Type int32 = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);

    Type uint8 = typeFactory.getIntrinsic(IntrinsicType.BYTE);
    Type uint16 = typeFactory.getIntrinsic(IntrinsicType.WORD);
    Type uint32 = typeFactory.getIntrinsic(IntrinsicType.CARDINAL);
    Type uint64 = typeFactory.getIntrinsic(IntrinsicType.UINT64);

    Type uint15 = ((TypeFactoryImpl) typeFactory).anonymousUInt15();
    Type uint31 = ((TypeFactoryImpl) typeFactory).anonymousUInt31();

    addAll(
        result,
        new OperatorIntrinsic(name, List.of(int8, int8), int8),
        new OperatorIntrinsic(name, List.of(int16, int16), int16),
        new OperatorIntrinsic(name, List.of(uint8, uint8), uint8),
        new OperatorIntrinsic(name, List.of(uint15, uint15), uint15),
        new OperatorIntrinsic(name, List.of(uint16, uint16), uint16),
        new OperatorIntrinsic(name, List.of(int32, int32), int32),
        new OperatorIntrinsic(name, List.of(int32, int64), int64),
        new OperatorIntrinsic(name, List.of(int64, int32), int64),
        new OperatorIntrinsic(name, List.of(uint31, uint31), uint31),
        new OperatorIntrinsic(name, List.of(uint32, uint31), uint32),
        new OperatorIntrinsic(name, List.of(uint31, uint32), uint32),
        new OperatorIntrinsic(name, List.of(uint32, uint32), uint32),
        new OperatorIntrinsic(name, List.of(int64, int64), int64),
        new OperatorIntrinsic(name, List.of(int64, uint64), uint64),
        new OperatorIntrinsic(name, List.of(uint64, int64), uint64),
        new OperatorIntrinsic(name, List.of(uint64, uint64), uint64));

    return result;
  }

  private Set<Invocable> createNativeIntegerBinary(String name) {
    Set<Invocable> result = Sets.newHashSet();

    Type nativeInt = typeFactory.getIntrinsic(IntrinsicType.NATIVEINT);
    Type nativeUInt = typeFactory.getIntrinsic(IntrinsicType.NATIVEUINT);

    if (!nativeInt.isWeakAlias()) {
      if (operands.stream().allMatch(o -> skipAlias(o).is(nativeInt))) {
        result.add(new OperatorIntrinsic(name, List.of(nativeInt, nativeInt), nativeInt));
      } else if (operands.stream().allMatch(o -> skipAlias(o).is(nativeUInt))) {
        result.add(new OperatorIntrinsic(name, List.of(nativeUInt, nativeUInt), nativeUInt));
      }
    }

    return result;
  }

  private Set<Invocable> createIntegerArithmeticBinary(String name) {
    Set<Invocable> result = createNativeIntegerBinary(name);
    if (!result.isEmpty()) {
      return result;
    }

    IntegerType integer = (IntegerType) typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);
    Type cardinal = typeFactory.getIntrinsic(IntrinsicType.CARDINAL);
    Type uint64 = typeFactory.getIntrinsic(IntrinsicType.UINT64);
    Type uint31 = ((TypeFactoryImpl) typeFactory).anonymousUInt31();

    addAll(
        result,
        new OperatorIntrinsic(name, List.of(integer, integer), integer),
        new OperatorIntrinsic(name, List.of(integer, int64), int64),
        new OperatorIntrinsic(name, List.of(int64, integer), int64),
        new OperatorIntrinsic(name, List.of(uint31, uint31), uint31),
        new OperatorIntrinsic(name, List.of(cardinal, uint31), cardinal),
        new OperatorIntrinsic(name, List.of(uint31, cardinal), cardinal),
        new OperatorIntrinsic(name, List.of(cardinal, cardinal), cardinal),
        new OperatorIntrinsic(name, List.of(integer, cardinal), int64),
        new OperatorIntrinsic(name, List.of(cardinal, integer), int64),
        new OperatorIntrinsic(name, List.of(int64, int64), int64),
        new OperatorIntrinsic(name, List.of(int64, uint64), uint64),
        new OperatorIntrinsic(name, List.of(uint64, int64), uint64),
        new OperatorIntrinsic(name, List.of(uint64, uint64), uint64));

    return result;
  }

  private static Type skipAlias(Type type) {
    while (type.isAlias()) {
      type = ((AliasType) type).aliasedType();
    }
    return type;
  }

  private Set<Invocable> createRealArithmeticBinary(String name) {
    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type extended = typeFactory.getIntrinsic(IntrinsicType.EXTENDED);

    return Sets.newHashSet(
        new OperatorIntrinsic(name, List.of(extended, extended), extended),
        new OperatorIntrinsic(name, List.of(integer, extended), extended),
        new OperatorIntrinsic(name, List.of(extended, integer), extended));
  }

  private Set<Invocable> createWithInterleavedTypes(String name, Type... types) {
    Set<Invocable> result = new HashSet<>();
    for (int i = 0; i < types.length; ++i) {
      Type type = types[i];
      result.add(new OperatorIntrinsic(name, List.of(type, type), type));
      if (i + 1 != types.length) {
        Type next = types[i + 1];
        result.add(new OperatorIntrinsic(name, List.of(type, next), next));
        result.add(new OperatorIntrinsic(name, List.of(next, type), next));
      }
    }
    return result;
  }

  private Set<Invocable> createDivide() {
    Type extended = typeFactory.getIntrinsic(IntrinsicType.EXTENDED);
    return Sets.newHashSet(new OperatorIntrinsic("Divide", List.of(extended, extended), extended));
  }

  private Set<Invocable> createLogical(String suffix) {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return Sets.newHashSet(new OperatorIntrinsic("Logical" + suffix, List.of(bool, bool), bool));
  }

  private Invocable createComparison(String name) {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return new OperatorIntrinsic(name, List.of(untypedType(), untypedType()), bool);
  }

  private Invocable createIn() {
    Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
    return new OperatorIntrinsic("In", List.of(ANY_ORDINAL, ANY_SET), bool);
  }

  private Set<Invocable> createShift(String prefix) {
    String name = prefix + "Shift";

    Type integer = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type nativeInt = typeFactory.getIntrinsic(IntrinsicType.NATIVEINT);
    Type nativeUInt = typeFactory.getIntrinsic(IntrinsicType.NATIVEUINT);

    if (!nativeInt.isWeakAlias()) {
      Type type = skipAlias(operands.get(0));
      if (type.is(nativeInt) || type.is(nativeUInt)) {
        return Sets.newHashSet(new OperatorIntrinsic(name, List.of(type, integer), type));
      }
    }

    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);
    Type cardinal = typeFactory.getIntrinsic(IntrinsicType.CARDINAL);
    Type uint64 = typeFactory.getIntrinsic(IntrinsicType.UINT64);
    Type uint31 = ((TypeFactoryImpl) typeFactory).anonymousUInt31();

    return Sets.newHashSet(
        new OperatorIntrinsic(name, List.of(integer, integer), integer),
        new OperatorIntrinsic(name, List.of(cardinal, integer), cardinal),
        new OperatorIntrinsic(name, List.of(uint31, integer), uint31),
        new OperatorIntrinsic(name, List.of(int64, integer), int64),
        new OperatorIntrinsic(name, List.of(uint64, integer), uint64));
  }

  private Set<Invocable> collectUnary(Type type, UnaryOperator operator) {
    Set<Invocable> result = new HashSet<>();

    if (type.isInteger()) {
      result.addAll(createIntegerUnary(operator));
    } else if (type.isReal()) {
      result.addAll(createRealUnary(operator));
    } else if (type.isBoolean()) {
      result.addAll(createBooleanUnary(operator));
    } else if (type.isVariant()) {
      result.add(createVariantUnary(operator));
    } else if (type.isStruct()) {
      result.addAll(collectOperatorOverloads((StructType) type, operator));
    }

    return result;
  }

  private Set<Invocable> createIntegerUnary(UnaryOperator operator) {
    Set<Invocable> result = Sets.newHashSet();

    switch (operator) {
      case NEGATE:
        result.addAll(createNegative());
        break;
      case PLUS:
        result.addAll(createPositive());
        break;
      case NOT:
        result.addAll(createBitwiseNot());
        break;
      default:
        // do nothing
    }

    return result;
  }

  private Set<Invocable> createNegative() {
    final String NAME = "Negative";

    Type operand = operands.get(0);

    if (operand.size() > 4) {
      return Sets.newHashSet(new OperatorIntrinsic(NAME, List.of(operand), operand));
    }

    Type int32 = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type uint32 = typeFactory.getIntrinsic(IntrinsicType.CARDINAL);
    Type int64 = typeFactory.getIntrinsic(IntrinsicType.INT64);

    return Sets.newHashSet(
        new OperatorIntrinsic(NAME, List.of(int32), int32),
        new OperatorIntrinsic(NAME, List.of(uint32), int64));
  }

  private Set<Invocable> createPositive() {
    final String NAME = "Positive";

    Type operand = operands.get(0);

    if (operand.size() >= 4) {
      return Sets.newHashSet(new OperatorIntrinsic(NAME, List.of(operand), operand));
    }

    Type int32 = typeFactory.getIntrinsic(IntrinsicType.INTEGER);
    Type uint31 = ((TypeFactoryImpl) typeFactory).anonymousUInt31();

    return Sets.newHashSet(
        new OperatorIntrinsic(NAME, List.of(int32), int32),
        new OperatorIntrinsic(NAME, List.of(uint31), uint31));
  }

  private Set<Invocable> createBitwiseNot() {
    Type operand = operands.get(0);
    return Sets.newHashSet(new OperatorIntrinsic("OnesComplement", List.of(operand), operand));
  }

  private Set<Invocable> createRealUnary(UnaryOperator operator) {
    Set<Invocable> result = Sets.newHashSet();

    Type extended = typeFactory.getIntrinsic(IntrinsicType.EXTENDED);

    switch (operator) {
      case PLUS:
        result.add(new OperatorIntrinsic("Positive", List.of(extended), extended));
        break;
      case NEGATE:
        result.add(new OperatorIntrinsic("Negative", List.of(extended), extended));
        break;
      default:
        // do nothing
    }

    return result;
  }

  private Set<Invocable> createBooleanUnary(UnaryOperator operator) {
    Set<Invocable> result = Sets.newHashSet();

    if (operator == UnaryOperator.NOT) {
      Type bool = typeFactory.getIntrinsic(IntrinsicType.BOOLEAN);
      result.add(new OperatorIntrinsic("LogicalNot", List.of(bool), bool));
    }

    return result;
  }

  private Invocable createVariantUnary(UnaryOperator operator) {
    final String PREFIX = "Variant::";
    Type variant = typeFactory.getIntrinsic(IntrinsicType.VARIANT);
    return new OperatorIntrinsic(PREFIX + operator.name(), List.of(variant, variant), variant);
  }
}
