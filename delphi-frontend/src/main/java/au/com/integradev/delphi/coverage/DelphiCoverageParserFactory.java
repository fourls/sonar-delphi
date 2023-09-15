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
package au.com.integradev.delphi.coverage;

import au.com.integradev.delphi.DelphiProperties;
import au.com.integradev.delphi.coverage.delphicodecoveragetool.DelphiCodeCoverageToolParser;
import au.com.integradev.delphi.msbuild.DelphiProjectHelper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;

@ScannerSide
public class DelphiCoverageParserFactory {
  private static final Logger LOG = LoggerFactory.getLogger(DelphiCoverageParserFactory.class);

  public Optional<DelphiCoverageParser> getParser(String key, DelphiProjectHelper helper) {
    if (DelphiProperties.COVERAGE_TOOL_DELPHI_CODE_COVERAGE.equals(key)) {
      return Optional.of(new DelphiCodeCoverageToolParser(helper));
    } else {
      LOG.warn("Unsupported coverage tool '{}'", key);
      return Optional.empty();
    }
  }
}
