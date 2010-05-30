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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import org.yawni.util.CharSequences;
import org.yawni.util.LightImmutableList;
import org.yawni.util.Utils;

public class FileManagerTest {
  //1 A 0..3 -- 3 is \n
  //3 C 4..7
  //4 D 8..11
  //5 E 12..15
  //6 F 16..19
//  @Ignore
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

//  @Ignore
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

//  @Ignore
  @Test
  public void testSearchesWithDups() throws IOException {
    final FileManagerInterface fm = new FileManager();
    final String path = "src/test/resources/testFileWithDups";
    String query;
    query = "4";
    assertEquals(query(query), "4 first 4", Utils.first(getMatches(query, fm, path)));
    query = "7";
//    assertEquals(query(query), null, getMatch(query, fm, path));
    assertEquals(true, Iterables.isEmpty(getMatches(query, fm, path)));
  }

  private static Iterable<CharSequence> getMatches(final String query, final FileManagerInterface fm, final String path) throws IOException {
    if (query.length() == 0) {
      return LightImmutableList.of();
    }
    // construct q s.t. it precisely preceeds query in sorted order, and leverage
    // feature of binary search returning (-insertion_point - 1) for non-matches
    final char last = query.charAt(query.length() - 1);
    final char prev = (char)(last - 1);
    String q = query.substring(0, query.length() - 1) + prev;
    System.err.println("query: "+query+" q: "+q);
    final int i = fm.getIndexedLinePointer(q, 0, path, false);
    // we're using modified query, so if it gets a hit (i >= 0),
    // we need to skip line(s) until actual match of original query hits
    int idx;
    if (i >= 0) {
      idx = i;
    } else {
      // i == -insertion_point - 1
      idx = 1 - i;
    }
    CharSequence line = fm.readLineAt(idx, path);
    
    System.err.println("line: "+line+" idx: "+idx+" i: "+i);
    final List<CharSequence> matches = Lists.newArrayList();
    int j;
    if (i < 0) {
      // has potential to match query (NOT q since q != query)
      assert ! CharSequences.startsWith(line, q);
      // advance j to next line
      j = idx + line.length() + 1;
    } else {
      assert i >= 0; // has potential to match q one or more times (NOT q since q != query)
      // skip all lines which match q
      j = idx; // loop will advance j if need be
      while (line != null && CharSequences.startsWith(line, q)) {
        j += (line.length() + 1);
        line = fm.readLineAt(j, path);
        System.err.println("line: "+line+" j: "+j);
      }
    }
    while (line != null && CharSequences.startsWith(line, query)) {
      matches.add(line);
      j += (line.length() + 1);
      line = fm.readLineAt(j, path);
    }
    return matches;
//    CharSequence line = fm.readLineAt(idx, path);
//    System.err.println("line: "+line+" idx: "+idx+" i: "+i);
//    final List<CharSequence> matches = Lists.newArrayList();
//    if (i >= 0) {
//      assert ! CharSequences.startsWith(line, q);
//    } else {
//      if (CharSequences.startsWith(line, query)) {
//        matches.add(line);
//      }
//    }
//    int j = idx + line.length() + 1;
//    line = fm.readLineAt(j, path);
//    System.err.println("next line: "+line+" j: "+j);
//    while (true) {
//      if (CharSequences.startsWith(line, query)) {
//        matches.add(line);
//        j = idx + line.length() + 1;
//        line = fm.readLineAt(j, path);
//      } else {
//        break;
//      }
//    }
//    return matches;
  }

  private static String getMatch(String query, final FileManagerInterface fm, final String path) throws IOException {
//    final char last = query.charAt(query.length() - 1);
//    final char prev = (char)(last - 1);
//    String q = query.substring(0, query.length() - 1) + prev;
//    System.err.println("query: "+query+" q: "+q);
//    final int i = fm.getIndexedLinePointer(q, 0, path, false);
//    // we're using modified query, so if it gets hit (i >= 0),
//    // we need to skip lines until actual match of original query hits
//    int idx;
//    if (i >= 0) {
//      idx = i;
//    } else {
//      // i == -(insertion point) - 1)
//      idx = 1 - i;
//    }
//    return fm.readLineAt(idx, path);
    final int i = fm.getIndexedLinePointer(query, 0, path, false);
    if (i >= 0) {
      return fm.readLineAt(i, path);
    } else {
      return null;
    }
  }

  private static String query(final String query) {
    return String.format("query: \"%s\"", query);
  }
}
