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
package org.yawni.wordnet;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class TeleologicalRelationTest {
  @Test
  public void test() {
//    System.err.println("values: "+TeleologicalRelation.getStringToRelMap());
    System.err.println("enum values: "+TeleologicalRelation.aliases());
    assertThat(TeleologicalRelation.AGENT).isSameAs(TeleologicalRelation.valueOf("AGENT"));
    assertThat(TeleologicalRelation.fromValue("AGENT")).isSameAs(TeleologicalRelation.valueOf("AGENT"));

    assertThat(TeleologicalRelation.fromValue("ACTION")).isSameAs(TeleologicalRelation.ACTION);
    assertThat(TeleologicalRelation.fromValue("action")).isSameAs(TeleologicalRelation.ACTION);
  }
}