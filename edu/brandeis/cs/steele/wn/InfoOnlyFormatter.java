package edu.brandeis.cs.steele.wn;

import java.util.logging.*;
/**
 * @author Luke Nezda
 */
class InfoOnlyFormatter extends SimpleFormatter {
  @Override public String format(final LogRecord record) {
    return new StringBuilder().
      append(String.format("%-7s", record.getLevel())).
      append(" ").
      //append(record.getSourceClassName()).append(" ").
      append(simplifyClassName(record.getSourceClassName())).append(".").
      append(record.getSourceMethodName()).append("() ").
      append(record.getMessage()).
      append("\n").toString();
  }
  @Override public String getHead(final Handler h) { return ""; }
  @Override public String getTail(final Handler h) { return ""; }
  static String simplifyClassName(String fullClassName) {
    final int idx = fullClassName.lastIndexOf(".");
    return fullClassName.substring(idx + 1);
  }
}

