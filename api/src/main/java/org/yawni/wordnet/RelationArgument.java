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

import java.util.List;

/**
 * A {@code RelationArgument} is the <em>source</em> or <em>target</em> of a {@link Relation}.
 * The target (and source) of a {@link SemanticRelation} is a {@link Synset};
 * the target (and source) of a {@link LexicalRelation} is a {@link WordSense}.
 * {@code RelationArgument} acts as common interface to {@code Synset} and {@code WordSense},
 * which form a composite pair, as evidenced by both being {@code Iterable<WordSense>} and
 * having {@link #getSynset()}.
 *
 * <p> This class used to be called {@code PointerTarget}.
 *
 * @see Relation
 * @see Synset
 * @see WordSense
 */
public interface RelationArgument extends Iterable<WordSense> {
  /**
   * Returns the outgoing {@link Relation}s from <em>this</em> argument, i.e., those
   * {@code Relation}s that have this object as their source.
   * <ul>
   *   <li>For a {@link WordSense}, this method returns all of the {@link LexicalRelation}s emanating from it,
   *   <em>and</em> all {@link SemanticRelation}s sourced at its {@link WordSense#getSynset() synset}.</li>
   * 
   *   <li>For a {@link Synset}, this method returns all {@link SemanticRelation}s sourced at it,
   *   <em>and</em> <em>all</em> {@link LexicalRelation}s emanating from <em>all</em> of its {@code WordSense}s.</li>
   * </ul>
   */
  List<Relation> getRelations();

  /** Filters {@link #getRelations() getRelations()} by type {@code type}. */
  List<Relation> getRelations(RelationType type);

  /**
   * Convenience filtered form of {@link RelationArgument#getRelations()}
   */
  List<SemanticRelation> getSemanticRelations(final RelationType type);

  /**
   * Convenience filtered form of {@link RelationArgument#getRelations()}
   */
  List<LexicalRelation> getLexicalRelations(final RelationType type);

  /**
   * Equivalent to {@code getSemanticRelations(null)}
   */
  default List<SemanticRelation> getSemanticRelations() {
    return getSemanticRelations(null);
  }

  /**
   * Equivalent to {@code getLexicalRelations(null)}
   */
  default List<LexicalRelation> getLexicalRelations() {
    return getLexicalRelations(null);
  }

  /** Returns the targets of the {@code Relation}s returned by {@link #getRelations() getRelations()}. */
  List<RelationArgument> getRelationTargets();

  /**
   * Returns the targets of the {@code Relation}s returned by
   * {@link #getRelations(RelationType) getRelations(RelationType)}
   */
  List<RelationArgument> getRelationTargets(RelationType type);

  /** {@code Synset} returns itself, {@code WordSense} returns its {@code Synset} */
  Synset getSynset();

  /**
   * If word does not intersect this argument, returns {@code null}, else
   * returns the apropos {@code WordSense}
   */
  WordSense getWordSense(Word word);

  /**
   * Returns the part of speech of this argument; notably will never be {@link POS#ALL}
   */
  POS getPOS();

  /**
   * Returns a description of this argument.  For a {@code WordSense}, this is
   * its lemma; for a {@code Synset}, it's the concatenated lemmas of
   * its {@code WordSense}s.
   */
  String getDescription();
}