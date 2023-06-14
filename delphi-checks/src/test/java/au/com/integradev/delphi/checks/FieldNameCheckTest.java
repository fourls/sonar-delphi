/*
 * Sonar Delphi Plugin
 * Copyright (C) 2015 Fabricio Colombo
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

class FieldNameCheckTest {
  @Test
  void testFieldWithValidNameShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class(TObject)")
                .appendDecl("  private")
                .appendDecl("   FFoo: Integer;")
                .appendDecl("  protected")
                .appendDecl("   FBar: String;")
                .appendDecl("  end;"))
        .verifyNoIssues();
  }

  @Test
  void testFieldNameWithoutPrefixShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class (TObject)")
                .appendDecl("    private")
                .appendDecl("     Id: Integer;")
                .appendDecl("     Code: Integer;")
                .appendDecl("    protected")
                .appendDecl("     Name: String;")
                .appendDecl("  end;"))
        .verifyIssueOnLine(8, 9, 11);
  }

  @Test
  void testPublicAndPublishedFieldsShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class")
                .appendDecl("     DefaultId: Integer;")
                .appendDecl("    private")
                .appendDecl("     Id: Integer;")
                .appendDecl("    protected")
                .appendDecl("     Name: String;")
                .appendDecl("    public")
                .appendDecl("     PublicName: String;")
                .appendDecl("  end;"))
        .verifyIssueOnLine(9, 11);
  }

  @Test
  void testPublicAndPublishedFieldsInMultipleClassesShouldNotAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class")
                .appendDecl("     DefaultId: Integer;")
                .appendDecl("    private")
                .appendDecl("     Id: Integer;")
                .appendDecl("    protected")
                .appendDecl("     Name: String;")
                .appendDecl("    public")
                .appendDecl("     PublicName: String;")
                .appendDecl("  end;")
                .appendDecl("type")
                .appendDecl("  TMyOtherClass = class")
                .appendDecl("     DefaultId: Integer;")
                .appendDecl("    private")
                .appendDecl("     Id: Integer;")
                .appendDecl("    protected")
                .appendDecl("     Name: String;")
                .appendDecl("    public")
                .appendDecl("     PublicName: String;")
                .appendDecl("  end;"))
        .verifyIssueOnLine(9, 11, 19, 21);
  }

  @Test
  void testBadPascalCaseShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class(TObject)")
                .appendDecl("    private")
                .appendDecl("     Ffoo: Integer;")
                .appendDecl("     Foo: Integer;")
                .appendDecl("    protected")
                .appendDecl("     Fbar: String;")
                .appendDecl("  end;"))
        .verifyIssueOnLine(8, 9, 11);
  }

  @Test
  void testOneLetterNameFieldsShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class")
                .appendDecl("    private")
                .appendDecl("     X: Integer;")
                .appendDecl("     F: Integer;")
                .appendDecl("  end;"))
        .verifyIssueOnLine(8, 9);
  }

  @Test
  void testMultipleFieldDeclarationWithBadNameShouldAddIssue() {
    CheckVerifier.newVerifier()
        .withCheck(new FieldNameCheck())
        .onFile(
            new DelphiTestUnitBuilder()
                .appendDecl("type")
                .appendDecl("  TMyClass = class(TObject)")
                .appendDecl("    private")
                .appendDecl("     X,")
                .appendDecl("     Y: Integer;")
                .appendDecl("  end;"))
        .verifyIssueOnLine(8, 9);
  }
}
