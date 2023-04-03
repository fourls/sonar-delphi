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
package au.com.integradev.delphi.reporting;

import au.com.integradev.delphi.DelphiProperties;
import au.com.integradev.delphi.check.MasterCheckRegistrar;
import au.com.integradev.delphi.check.ScopeMetadataLoader;
import au.com.integradev.delphi.file.DelphiFile.DelphiInputFile;
import au.com.integradev.delphi.type.factory.TypeFactory;
import au.com.integradev.delphi.utils.TextRangeUtils;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.communitydelphi.api.ast.DelphiAst;
import org.sonar.plugins.communitydelphi.api.ast.DelphiNode;
import org.sonar.plugins.communitydelphi.api.ast.MethodImplementationNode;
import org.sonar.plugins.communitydelphi.api.ast.TypeDeclarationNode;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheck;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheckContext;
import org.sonar.plugins.communitydelphi.api.check.DelphiCheckContext.Location;
import org.sonar.plugins.communitydelphi.api.check.FilePosition;
import org.sonar.plugins.communitydelphi.api.symbol.declaration.TypeNameDeclaration;
import org.sonar.plugins.communitydelphi.api.symbol.scope.DelphiScope;
import org.sonar.plugins.communitydelphi.api.symbol.scope.TypeScope;
import org.sonar.plugins.communitydelphi.api.type.Type;
import org.sonar.plugins.communitydelphi.api.type.Type.ScopedType;

/**
 * Based directly on {@code InternalJavaIssueBuilder} from the sonar-java project.
 *
 * @see <a
 *     href="https://github.com/SonarSource/sonar-java/blob/master/java-frontend/src/main/java/org/sonar/java/reporting/InternalJavaIssueBuilder.java">
 *     InternalJavaIssueBuilder </a>
 */
public final class DelphiIssueBuilder {
  private static final Logger LOG = Loggers.get(DelphiIssueBuilder.class);

  private static final String RULE_NAME = "rule";
  private static final String POSITION_NAME = "position";
  private static final String MESSAGE_NAME = "message";
  private static final String FLOWS_NAME = "flows";
  private static final String SECONDARIES_NAME = "secondaries";

  private final SensorContext context;
  private final DelphiInputFile delphiFile;
  private final MasterCheckRegistrar checkRegistrar;
  private final ScopeMetadataLoader scopeMetadataLoader;
  private DelphiCheck rule;
  private FilePosition position;
  private String message;
  @Nullable private List<DelphiCheckContext.Location> secondaries;
  @Nullable private List<List<Location>> flows;
  @Nullable private Integer cost;
  private boolean reported;

  public DelphiIssueBuilder(
      SensorContext context,
      DelphiInputFile delphiFile,
      MasterCheckRegistrar checkRegistrar,
      ScopeMetadataLoader scopeMetadataLoader) {
    this.context = context;
    this.delphiFile = delphiFile;
    this.checkRegistrar = checkRegistrar;
    this.scopeMetadataLoader = scopeMetadataLoader;
  }

  private static void requiresValueToBeSet(Object target, String targetName) {
    Preconditions.checkState(target != null, "A %s must be set first.", targetName);
  }

  private static void requiresValueNotToBeSet(Object target, String targetName, String otherName) {
    Preconditions.checkState(
        target == null, "Cannot set %s when %s is already set.", targetName, otherName);
  }

  private static void requiresSetOnlyOnce(Object target, String targetName) {
    Preconditions.checkState(target == null, "Cannot set %s multiple times.", targetName);
  }

  public DelphiIssueBuilder forRule(DelphiCheck rule) {
    requiresSetOnlyOnce(this.rule, RULE_NAME);

    this.rule = rule;
    return this;
  }

  public DelphiIssueBuilder onNode(DelphiNode node) {
    return onFilePosition(FilePosition.from(node));
  }

  public DelphiIssueBuilder onRange(DelphiNode startNode, DelphiNode endNode) {
    return onFilePosition(
        FilePosition.from(
            startNode.getBeginLine(),
            startNode.getBeginColumn(),
            endNode.getEndLine(),
            endNode.getEndColumn()));
  }

  public DelphiIssueBuilder onFilePosition(FilePosition position) {
    this.position = position;
    return this;
  }

  public DelphiIssueBuilder withMessage(String message) {
    this.message = message;
    return this;
  }

  @FormatMethod
  public DelphiIssueBuilder withMessage(@FormatString String message, Object... args) {
    this.message = String.format(message, args);
    return this;
  }

  public DelphiIssueBuilder withSecondaries(DelphiCheckContext.Location... secondaries) {
    return withSecondaries(Arrays.asList(secondaries));
  }

  public DelphiIssueBuilder withSecondaries(List<DelphiCheckContext.Location> secondaries) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);
    requiresValueNotToBeSet(this.flows, FLOWS_NAME, SECONDARIES_NAME);
    requiresSetOnlyOnce(this.secondaries, SECONDARIES_NAME);

    this.secondaries = Collections.unmodifiableList(secondaries);
    return this;
  }

  public DelphiIssueBuilder withFlows(List<List<DelphiCheckContext.Location>> flows) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);
    requiresValueNotToBeSet(this.secondaries, SECONDARIES_NAME, FLOWS_NAME);
    requiresSetOnlyOnce(this.flows, FLOWS_NAME);

    this.flows = Collections.unmodifiableList(flows);
    return this;
  }

  public DelphiIssueBuilder withCost(int cost) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);
    requiresSetOnlyOnce(this.cost, "cost");

    this.cost = cost;
    return this;
  }

  public void report() {
    Preconditions.checkState(!reported, "Can only be reported once.");
    requiresValueToBeSet(rule, RULE_NAME);
    requiresValueToBeSet(position, POSITION_NAME);
    requiresValueToBeSet(message, MESSAGE_NAME);

    if (isOutOfScope()) {
      return;
    }

    Optional<RuleKey> ruleKey = checkRegistrar.getRuleKey(rule);
    if (ruleKey.isEmpty()) {
      LOG.trace("Rule not enabled - discarding issue");
      return;
    }

    NewIssue newIssue =
        context.newIssue().forRule(ruleKey.get()).gap(cost == null ? 0 : cost.doubleValue());

    InputFile inputFile = delphiFile.getInputFile();

    newIssue.at(
        newIssue
            .newLocation()
            .on(inputFile)
            .at(
                inputFile.newRange(
                    position.getBeginLine(),
                    position.getBeginColumn(),
                    position.getEndLine(),
                    position.getEndColumn()))
            .message(message));

    if (secondaries != null) {
      // Transform secondaries into flows
      // Keep secondaries and flows mutually exclusive.
      flows = secondaries.stream().map(Collections::singletonList).collect(Collectors.toList());
      secondaries = null;
    }

    if (flows != null) {
      for (List<DelphiCheckContext.Location> flow : flows) {
        newIssue.addFlow(
            flow.stream()
                .map(location -> createNewIssueLocation(inputFile, newIssue, location))
                .collect(Collectors.toList()));
      }
    }

    newIssue.save();
    reported = true;
  }

  private static NewIssueLocation createNewIssueLocation(
      InputFile inputFile, NewIssue newIssue, Location location) {
    return newIssue
        .newLocation()
        .on(inputFile)
        .at(TextRangeUtils.fromFilePosition(location.getFilePosition(), inputFile))
        .message(location.getMessage());
  }

  private boolean isOutOfScope() {
    switch (scopeMetadataLoader.getScope(rule.getClass())) {
      case MAIN:
        return isWithinTestCode();
      case TEST:
        return !isWithinTestCode();
      default:
        return false;
    }
  }

  private boolean isWithinTestCode() {
    Type type = findEnclosingType(position);
    return isTestType(type) || isNestedInsideTestType(type);
  }

  private boolean isTestType(Type type) {
    return context
        .config()
        .get(DelphiProperties.TEST_SUITE_TYPE_KEY)
        .map(
            testSuiteTypeImage ->
                type.is(testSuiteTypeImage) || type.isSubTypeOf(testSuiteTypeImage))
        .orElse(false);
  }

  private boolean isNestedInsideTestType(Type type) {
    if (type instanceof ScopedType) {
      DelphiScope scope = ((ScopedType) type).typeScope();
      while ((scope = scope.getParent()) instanceof TypeScope) {
        if (isTestType(((TypeScope) scope).getType())) {
          return true;
        }
      }
    }
    return false;
  }

  private Type findEnclosingType(FilePosition filePosition) {
    Optional<TypeDeclarationNode> typeDeclarationNode =
        findNodeEnclosingFilePosition(TypeDeclarationNode.class, filePosition);

    if (typeDeclarationNode.isPresent()) {
      return typeDeclarationNode.get().getType();
    }

    Optional<MethodImplementationNode> methodImplementationNode =
        findNodeEnclosingFilePosition(MethodImplementationNode.class, filePosition);

    if (methodImplementationNode.isPresent()) {
      TypeNameDeclaration typeDeclaration = methodImplementationNode.get().getTypeDeclaration();
      if (typeDeclaration != null) {
        return typeDeclaration.getType();
      }
    }

    return TypeFactory.unknownType();
  }

  private <T extends DelphiNode> Optional<T> findNodeEnclosingFilePosition(
      Class<T> nodeClass, FilePosition filePosition) {
    DelphiAst ast = delphiFile.getAst();
    return ast.findDescendantsOfType(nodeClass).stream()
        .filter(
            node ->
                node.getBeginLine() >= filePosition.getBeginLine()
                    && node.getBeginColumn() >= filePosition.getBeginColumn()
                    && node.getEndLine() <= filePosition.getEndLine()
                    && node.getEndColumn() <= filePosition.getEndColumn())
        .max(
            (a, b) ->
                new CompareToBuilder()
                    .append(a.getBeginLine(), b.getBeginLine())
                    .append(a.getBeginColumn(), b.getBeginColumn())
                    .toComparison());
  }
}