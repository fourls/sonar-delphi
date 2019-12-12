/*
 * Sonar Delphi Plugin
 * Copyright (C) 2011 Sabre Airline Solutions and Fabricio Colombo
 * Author(s):
 * Przemyslaw Kociolek (przemyslaw.kociolek@sabre.com)
 * Michal Wojcik (michal.wojcik@sabre.com)
 * Fabricio Colombo (fabricio.colombo.mva@gmail.com)
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
package org.sonar.plugins.delphi.project;

import static org.sonar.plugins.delphi.utils.DelphiUtils.resolvePathFromBaseDir;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Class for parsing .dproj xml file */
public class DelphiProjectXmlParser extends DefaultHandler {
  private static final Logger LOG = Loggers.get(DelphiProjectXmlParser.class);

  private final String fileName;
  private final Path baseDir;
  private final DelphiProject project;
  private boolean isReading;
  private String readData;

  /**
   * C-tor
   *
   * @param xml Xml file to parse
   * @param delphiProject DelphiProject class to modify
   */
  DelphiProjectXmlParser(Path xml, DelphiProject delphiProject) {
    fileName = xml.toAbsolutePath().toString();
    baseDir = xml.getParent();
    project = delphiProject;
  }

  /** Parses the document */
  void parse() {
    LOG.debug("Indexing project file: {}", fileName);
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      SAXParser parser = factory.newSAXParser();
      parser.parse(fileName, this);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      LOG.error("{}: Error while parsing project file: ", fileName, e);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    if (isReading) {
      readData = new String(ch.clone(), start, length);
    }
  }

  @Override
  public void startElement(String uri, String localName, String rawName, Attributes attributes) {
    isReading = false;
    switch (rawName) {
      case "DCCReference":
        handleDCCReferenceStart(attributes);
        break;
      case "VersionInfoKeys":
        handleVersionInfoKeysStart(attributes);
        break;
      case "DCC_UnitSearchPath":
      case "DCC_Namespace":
      case "DCC_Define":
        isReading = true;
        break;
      default:
        // Do nothing
    }
  }

  @Override
  public void endElement(String uri, String localName, String rawName) {
    if (!isReading) {
      return;
    }

    switch (rawName) {
      case "VersionInfoKeys":
        handleVersionInfoKeysEnd();
        break;
      case "DCC_UnitSearchPath":
        handleUnitSearchPathEnd();
        break;
      case "DCC_Namespace":
        handleNamespaceEnd();
        break;
      case "DCC_Define":
        handleDefineEnd();
        break;
      default:
        // Do nothing
    }
  }

  private void handleDCCReferenceStart(Attributes attributes) {
    Path sourceFile = resolvePathFromBaseDir(baseDir, Path.of(attributes.getValue("Include")));
    project.addSourceFile(sourceFile);
  }

  private void handleVersionInfoKeysStart(Attributes attributes) {
    String name = attributes.getValue("Name");
    if ("ProductName".equals(name)) {
      isReading = true;
    }
  }

  private void handleVersionInfoKeysEnd() {
    project.setName(readData);
  }

  private void handleUnitSearchPathEnd() {
    Iterable<String> paths = Splitter.on(';').split(readData);
    for (String path : paths) {
      if (path.startsWith("$")) {
        continue;
      }
      Path searchPathDirectory = resolvePathFromBaseDir(baseDir, Path.of(path));
      project.addSearchDirectory(searchPathDirectory);
    }
  }

  private void handleNamespaceEnd() {
    Iterable<String> unitScopeNames = Splitter.on(';').split(readData);
    for (String unitScopeName : unitScopeNames) {
      if (unitScopeName.startsWith("$")) {
        continue;
      }
      project.addUnitScopeName(unitScopeName);
    }
  }

  private void handleDefineEnd() {
    Iterable<String> defines = Splitter.on(';').split(readData);
    for (String define : defines) {
      if (define.startsWith("$") || "DEBUG".equals(define)) {
        continue;
      }
      project.addDefinition(define);
    }
  }
}