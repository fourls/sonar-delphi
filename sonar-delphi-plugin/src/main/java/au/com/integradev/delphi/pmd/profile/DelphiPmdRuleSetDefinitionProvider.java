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
package au.com.integradev.delphi.pmd.profile;

import static java.nio.charset.StandardCharsets.UTF_8;

import au.com.integradev.delphi.pmd.DelphiPmdConstants;
import au.com.integradev.delphi.pmd.xml.DelphiRuleSet;
import au.com.integradev.delphi.pmd.xml.DelphiRuleSetHelper;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@ServerSide
@SonarLintSide
public class DelphiPmdRuleSetDefinitionProvider {

  private DelphiRuleSet definition;

  public DelphiRuleSet getDefinition() {
    if (definition == null) {
      InputStream rulesResource = getClass().getResourceAsStream(DelphiPmdConstants.RULES_XML);
      Reader rulesReader = new InputStreamReader(rulesResource, UTF_8);
      definition = DelphiRuleSetHelper.createFrom(rulesReader);
    }

    return definition;
  }
}