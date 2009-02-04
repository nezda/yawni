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
package edu.brandeis.cs.steele.wn;

import java.util.logging.*;
/**
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

