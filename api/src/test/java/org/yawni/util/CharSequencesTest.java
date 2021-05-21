/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharSequencesTest {
  @Test
  public void testHashCode() {
    assertEquals("1".hashCode(), CharSequences.hashCode(new StringBuilder("1")));
    assertEquals("-1557994400".hashCode(), CharSequences.hashCode(new StringBuilder("-1557994400")));
  }

//  @Test
//  public void testEquals() {
//  }
//
//  @Test
//  public void testStartsWith() {
//  }
//
//  @Test
//  public void testRegionMatches_5args() {
//  }
//
//  @Test
//  public void testRegionMatches_6args() {
//  }
//
//  @Test
//  public void testContainsUpper() {
//  }
//
//  @Test
//  public void testSameLetterDigitSequence() {
//  }
//
//  @Test
//  public void testParseInt_CharSequence_int() {
//  }
//
//  @Test
//  public void testParseInt_3args() {
//  }
//
//  @Test
//  public void testParseInt_4args() {
//  }
//
//  @Test
//  public void testParseLong_CharSequence_int() {
//  }
//
//  @Test
//  public void testParseLong_4args() {
//  }
//
//  @Test
//  public void testSubstring() {
//  }
}