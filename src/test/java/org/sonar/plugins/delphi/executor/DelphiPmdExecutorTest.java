package org.sonar.plugins.delphi.executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.delphi.pmd.DelphiPmdConstants.BASE_EFFORT;
import static org.sonar.plugins.delphi.pmd.DelphiPmdConstants.SCOPE;
import static org.sonar.plugins.delphi.pmd.DelphiPmdConstants.TYPE;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.plugins.delphi.pmd.DelphiPmdConfiguration;
import org.sonar.plugins.delphi.pmd.DelphiPmdConstants;
import org.sonar.plugins.delphi.pmd.profile.DelphiPmdRuleSetDefinitionProvider;
import org.sonar.plugins.delphi.pmd.violation.DelphiPmdViolationRecorder;
import org.sonar.plugins.delphi.pmd.violation.DelphiRuleViolation;
import org.sonar.plugins.delphi.pmd.xml.DelphiRule;
import org.sonar.plugins.delphi.pmd.xml.DelphiRuleSet;
import org.sonar.plugins.delphi.utils.DelphiUtils;

public class DelphiPmdExecutorTest {

  private static final String ROOT_PATH = "/org/sonar/plugins/delphi";
  private static final File ROOT_DIR = DelphiUtils.getResource(ROOT_PATH);

  private SensorContext context;
  private DelphiPmdExecutor executor;
  private DelphiPmdConfiguration pmdConfiguration;
  private DelphiPmdViolationRecorder violationRecorder;

  @Rule public ExpectedException exceptionCatcher = ExpectedException.none();

  @Before
  public void setup() {
    DefaultFileSystem fileSystem = new DefaultFileSystem(ROOT_DIR).setWorkDir(ROOT_DIR.toPath());
    Configuration configuration = mock(Configuration.class);
    DelphiPmdRuleSetDefinitionProvider provider = new DelphiPmdRuleSetDefinitionProvider();

    context = spy(SensorContextTester.create(ROOT_DIR));
    ActiveRules rules = mock(ActiveRules.class);
    pmdConfiguration = spy(new DelphiPmdConfiguration(fileSystem, configuration, provider));
    violationRecorder = mock(DelphiPmdViolationRecorder.class);

    executor = spy(new DelphiPmdExecutor(context, rules, pmdConfiguration, violationRecorder));
    executor.setup();
  }

  @Test
  public void testNonexistentRuleSetIllegalState() {
    File badFile = mock(File.class);
    when(badFile.getAbsolutePath()).thenReturn("does/not/exist.xml");
    when(pmdConfiguration.dumpXmlRuleSet(anyString(), anyString())).thenReturn(badFile);

    exceptionCatcher.expect(IllegalStateException.class);
    executor.setup();
  }

  @Test
  public void testAddBuiltinProperties() {
    // The SwallowedExceptionsRule has 3 builtin properties: BASE_EFFORT, SCOPE and TYPE
    DelphiRule rule = new DelphiRule();
    rule.setName("SwallowedExceptionsRule");
    rule.setClazz("org.sonar.plugins.delphi.pmd.rules.SwallowedExceptionsRule");
    rule.setPriority(2);

    DelphiRuleSet ruleSet = new DelphiRuleSet();
    ruleSet.addRule(rule);

    executor.addBuiltinProperties(ruleSet);

    assertThat(rule.getProperties(), hasSize(3));
    assertThat(rule.getProperty(BASE_EFFORT.name()), is(not(nullValue())));
    assertThat(rule.getProperty(SCOPE.name()), is(not(nullValue())));
    assertThat(rule.getProperty(TYPE.name()), is(not(nullValue())));
  }

  @Test
  public void testAddBuiltinPropertiesToCustomRule() {
    // A custom template rule, created via the SonarQube web interface
    // The XPathRule template has 2 builtin properties: BASE_EFFORT and TEMPLATE
    // The TEMPLATE property should not be inherited by the custom rule
    DelphiRule rule = new DelphiRule();
    rule.setName("SomeCustomXPathRule");
    rule.setTemplateName("XPathRule");
    rule.setClazz(DelphiPmdConstants.TEMPLATE_XPATH_CLASS);
    rule.setPriority(2);

    DelphiRuleSet ruleSet = new DelphiRuleSet();
    ruleSet.addRule(rule);

    executor.addBuiltinProperties(ruleSet);

    assertThat(rule.getProperties(), hasSize(1));
    assertThat(rule.getProperty(BASE_EFFORT.name()), is(not(nullValue())));
  }

  @Test
  public void testAddBuiltinPropertiesToNonexistentRule() {
    exceptionCatcher.expect(IllegalStateException.class);

    // An undefined rule in ActiveRules should be impossible
    // Undefined -> The rule is not specified in rules.xml, either concretely or as a template
    DelphiRule rule = new DelphiRule();
    rule.setName("NonexistentRule");
    rule.setClazz("org.sonar.plugins.delphi.pmd.rules.NonexistentRule");
    rule.setPriority(2);

    DelphiRuleSet ruleSet = new DelphiRuleSet();
    ruleSet.addRule(rule);

    executor.addBuiltinProperties(ruleSet);
  }

  @Test
  public void testShouldNotReportZeroViolations() {
    // The report is empty if executor.execute(DelphiFile) is never called
    executor.complete();

    verify(violationRecorder, never()).saveViolation(any(DelphiRuleViolation.class), eq(context));
    verifyZeroInteractions(context);
  }
}