package edu.brandeis.cs.steele.wn.browser;

import java.util.jar.*;
import java.util.*;
import java.io.*;

/** 
 * Reads a properties file (target/classes/) containing the
 * of the buildnumber-maven-plugin.
 */
class Application {
  private final String applicationName;
  private final String applicationVersion;
  private final String buildNumber;
  private final Date buildDate;

  Application() {
    try {
      final Properties props = new Properties();
      final InputStream in = getClass().getClassLoader().getResourceAsStream(
        getPackagePath()+"/"+"application.properties");
      if(in == null) {
        throw new RuntimeException("resource not found");
      }
      props.load(in);
      in.close();
      this.applicationName = (String) props.get("application.name");
      this.applicationVersion = (String) props.get("application.version");
      this.buildNumber = (String) props.get("application.buildNumber");
      final String buildDateString = (String) props.get("application.buildDate");
      this.buildDate = new Date(Long.valueOf(buildDateString));

      //System.err.println("getPackage(): "+getPackagePath());
      //System.err.println("props: "+props);
    } catch(IOException ioe) {
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

  @Override public String toString() {
    return new StringBuilder().
      append("[").
      append(getName()).
      append(" ").
      append(getVersion()).
      append(" ").
      append(getBuildNumber()).
      append(" ").
      append(getFormattedBuildDate()).
      append("]").
      toString();
  }

  private String getPackagePath() {
    return getClass().getPackage().getName().replace(".", "/");
  }
}
