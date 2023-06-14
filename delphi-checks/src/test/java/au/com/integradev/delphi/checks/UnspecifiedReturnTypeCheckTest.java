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
package au.com.integradev.delphi.checks;

import au.com.integradev.delphi.builders.DelphiTestUnitBuilder;
import au.com.integradev.delphi.checks.verifier.CheckVerifier;
import org.junit.jupiter.api.Test;

class UnspecifiedReturnTypeCheckTest {
  @Test
  void testFunctionWithReturnTypeShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new UnspecifiedReturnTypeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("function TClass.MyFunction: String;")
                .appendImpl("begin")
                .appendImpl("  Result := 'MyString';")
                .appendImpl("end;"))
        .verifyNoIssues();
  }

  @Test
  void testFunctionWithoutReturnTypeShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new UnspecifiedReturnTypeCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendImpl("function TClass.MyFunction;")
                .appendImpl("begin")
                .appendImpl("  Result := 'MyString';")
                .appendImpl("end;"))
        .verifyIssueOnLine(7);
  }
}
