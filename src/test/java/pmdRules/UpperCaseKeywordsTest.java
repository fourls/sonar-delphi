package pmdRules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

public class UpperCaseKeywordsTest extends BaseXmlPmdRuleTest {

  private static String ruleType = "UppercaseReservedKeywordsRule";

  @Test
  public void testRuleViolation() {

    String testFile = "KeywordsConventionTest.pas";
    super.testAnalyse();
    ArrayList<ArrayList<Object>> fileResults = getFileResults(testFile);

    Integer[] expectedViolationLines = {34};

    ArrayList violationLines = super.getViolationLines(fileResults, ruleType);

    assertEquals(violationLines, Arrays.asList(expectedViolationLines));
  }

  @Test
  public void testNoViolation() {

    String testFile = "DestructorNoViolationTest.pas";
    super.testAnalyse();
    ArrayList<ArrayList<Object>> fileResults = getFileResults(testFile);

    assertNull(fileResults); // Shouldn't be any violations in this file
  }

}