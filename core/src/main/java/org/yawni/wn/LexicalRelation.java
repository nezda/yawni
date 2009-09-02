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
package org.yawni.wn;

/**
 * A <code>LexicalRelation</code> encodes a lexical relationship between {@link WordSense}s.
 */
public final class LexicalRelation extends Relation {
  LexicalRelation(final int targetOffset, final int targetIndex, final byte targetPOSOrdinal,
    final int index, final RelationTarget source, final byte relationTypeOrdinal) {
    super(targetOffset, targetIndex, targetPOSOrdinal, index, source, relationTypeOrdinal);
    assert getSource() instanceof WordSense;
    // can't call getTarget() - infinite recursion
  }

  @Override
  public WordSense getSource() {
    @SuppressWarnings("unchecked")
    final WordSense source = (WordSense) super.getSource();
    return source;
  }

  @Override
  public WordSense getTarget() {
    @SuppressWarnings("unchecked")
    final WordSense target = (WordSense) super.getTarget();
    return target;
  }
}