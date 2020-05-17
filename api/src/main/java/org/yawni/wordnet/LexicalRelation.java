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
 * A {@code LexicalRelation} encodes a lexical relationship between {@link WordSense}s.
 */
public final class LexicalRelation extends Relation {
  LexicalRelation(final int targetOffset, final int targetIndex, final POS targetPOS, final WordNet wordNet,
      final int sourceRelationIndex, final int sourceOffset, final int sourceIndex, final POS sourcePOS,
      final RelationType relationType) {
    super(targetOffset, targetIndex, targetPOS, wordNet,
        sourceRelationIndex, sourceOffset, sourceIndex, sourcePOS, relationType);
  }

  /**
   * Copy constructor to create Relation with equal source and target, but different type
   */
  LexicalRelation(final LexicalRelation that, final RelationType relationType, final int relationIndex) {
    super(that, relationType, relationIndex);
  }

  @Override
  public boolean isLexical() {
    return true;
  }

  @Override
  public boolean isSemantic() {
    return false;
  }

  @Override
  public WordSense getSource() {
    @SuppressWarnings("unchecked")
    final WordSense source = (WordSense) super.getSource();
    return source;
  }

  @Override
  public boolean hasSource(final RelationArgument that) {
    return that instanceof WordSense
        && that.getSynset().getOffset() == this.getSourceOffset()
        && that.getSynset().getPOS() == this.getSourcePOS()
        //FIXME incomplete for WordSense
        ;
  }

  @Override
  public WordSense getTarget() {
    @SuppressWarnings("unchecked")
    final WordSense target = (WordSense) super.getTarget();
    return target;
  }
}