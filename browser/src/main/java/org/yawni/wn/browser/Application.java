/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wn.browser;

import java.util.*;
import java.io.*;

/** 
 * Reads a properties file (target/classes/) containing the
 * output of the buildnumber-maven-plugin.
 */
final class Application {
  private final String applicationName;
  private final String moduleName;
  private final String artifactId;
  private final String applicationVersion;
  private final String buildNumber;
  private final Date buildDate;

  Application() {
    try {
      final Properties props = new Properties();
      final InputStream in = getClass().getClassLoader().getResourceAsStream(
        getPackagePath() + "/" + "application.properties");
      if (in == null) {
        throw new RuntimeException("resource not found");
      }
      props.load(in);
      in.close();
      this.applicationName = (String) props.get("application.name");
      this.moduleName = (String) props.get("application.moduleName");
      this.artifactId = (String) props.get("application.artifactId");
      this.applicationVersion = (String) props.get("application.version");
      this.buildNumber = (String) props.get("application.buildNumber");
      final String buildDateString = (String) props.get("application.buildDate");
      this.buildDate = new Date(Long.valueOf(buildDateString));
      //System.err.println("getPackage(): "+getPackagePath());
      //System.err.println("props: "+props);
      //System.err.println(this);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static Application instance;
  
  static Application getInstance() {
    if (instance == null) {
      instance = new Application();
    }
    return instance;
  }

  public String getName() {
    return applicationName;
  }

  public String getModuleName() {
    return moduleName;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return applicationVersion;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public Date getBuildDate() {
    return buildDate;
  }

  public String getFormattedBuildDate() {
    return String.format("%tY-%tm-%td", getBuildDate(), getBuildDate(), getBuildDate());
  }

  @Override
  public String toString() {
    return new StringBuilder().
      append('[').
      append(getName()).
      //append(' ').
      //append(getArtifactId()).
      append(' ').
      append(getVersion()).
      append(' ').
      append(getBuildNumber()).
      append(' ').
      append(getFormattedBuildDate()).
      append(']').
      toString();
  }

  private String getPackagePath() {
    return getClass().getPackage().getName().replace('.', '/');
  }
}