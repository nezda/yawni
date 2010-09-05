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

/**
 * A {@code SemanticRelation} encodes a lexical relationship between {@link Synset}s.
 */
public final class SemanticRelation extends Relation {
  SemanticRelation(final int targetOffset, final int targetIndex, final byte targetPOSOrdinal,
    final int srcRelationIndex, final RelationArgument source, final byte relationTypeOrdinal) {
    super(targetOffset, targetIndex, targetPOSOrdinal, srcRelationIndex, source, relationTypeOrdinal);
    assert super.getSource() instanceof Synset;
    // can't call getTarget() - infinite recursion
  }

  /** {@inheritDoc} */
  @Override
  public Synset getSource() {
    @SuppressWarnings("unchecked")
    final Synset source = (Synset) super.getSource();
    return source;
  }

  /** {@inheritDoc} */
  @Override
  public Synset getTarget() {
    @SuppressWarnings("unchecked")
    final Synset target = (Synset) super.getTarget();
    return target;
  }
}