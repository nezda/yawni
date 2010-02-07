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
package org.yawni.wordnet;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;

public class FileManagerTest {
  //1 A 0..3 -- 3 is \n
  //3 C 4..7
  //4 D 8..11
  //5 E 12..15
  //6 F 16..19
  @Test
  public void testSearches() throws IOException {
    final FileManagerInterface fm = new FileManager();
    final String path = "src/test/resources/testFile";
    //final String path = "testFile";
    String query;
    //query = "2";
    //final int offset = fm.getIndexedLinePointer(query, path, false);
    //System.err.println("found offset: "+offset);
    query = "3";
    assertEquals(query(query), 4, fm.getIndexedLinePointer(query, 0, path, false));
    // tests non-zero start idx
    query = "3";
    assertEquals(query(query), 4, fm.getIndexedLinePointer(query, 4, path, false));
    query = "1";
    assertEquals(query(query), 0, fm.getIndexedLinePointer(query, 0, path, false));
    query = "4";
    assertEquals(query(query), 8, fm.getIndexedLinePointer(query, 0, path, false));
    query = "2";
    assertEquals(query(query), -5, fm.getIndexedLinePointer(query, 0, path, false));
    query = "7";
    assertEquals(query(query), -21, fm.getIndexedLinePointer(query, 0, path, false));
  }

  @Test
  public void testMoreSearches() throws IOException {
    final FileManagerInterface fm = new FileManager();
    final String path = "src/test/resources/harderTestFile";
    String query;
    query = "3";
    assertEquals(query(query), 27, fm.getIndexedLinePointer(query, 0, path, false));
    query = "1";
    assertEquals(query(query), 0, fm.getIndexedLinePointer(query, 0, path, false));
    query = "4";
    assertEquals(query(query), 81, fm.getIndexedLinePointer(query, 0, path, false));
    query = "2";
    assertEquals(query(query), -28, fm.getIndexedLinePointer(query, 0, path, false));
    query = "7";
    assertEquals(query(query), -145, fm.getIndexedLinePointer(query, 0, path, false));
    query = "88";
    assertEquals(query(query), 144, fm.getIndexedLinePointer(query, 0, path, false));
    query = "8";
    assertEquals(query(query), -145, fm.getIndexedLinePointer(query, 0, path, false));
  }

  static String query(final String query) {
    return String.format("query: \"%s\"", query);
  }
}
