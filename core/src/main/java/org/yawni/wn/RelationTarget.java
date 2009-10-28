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

import java.util.List;

/**
 * A {@code RelationTarget} is the <em>source</em> or <em>target</em> of a {@link Relation}.
 * The target (and source) of a {@link SemanticRelation} is a {@link Synset};
 * the target (and source) of a {@link LexicalRelation} is a {@link WordSense}.
 *
 * <p> Note this class used to be called {@code PointerTarget}.
 *
 * @see Relation
 * @see Synset
 * @see WordSense
 */
public interface RelationTarget extends Iterable<WordSense> {
  public POS getPOS();

  /**
   * Returns a description of the target.  For a {@code WordSense}, this is
   * its lemma; for a {@code Synset}, it's the concatenated lemma's of
   * its {@code WordSense}s.
   */
  public String getDescription();

  /**
   * Returns a long description of the target.  This is its description,
   * appended by, if it exists, a dash and its gloss.
   */
  public String getLongDescription();

  /**
   * Returns the outgoing {@code Relation}s from this target -- those
   * {@code Relation}s that have this object as their source.
   */
  public List<? extends Relation> getRelations();

  /** Returns the outgoing {@code Relation}s of type {@code type}. */
  public List<? extends Relation> getRelations(RelationType type);

  /** Returns the targets of the outgoing {@code Relation}s. */
  public List<RelationTarget> getTargets();

  /**
   * Returns the targets of the outgoing {@code Relation}s that have type
   * {@code type}.
   */
  public List<RelationTarget> getTargets(RelationType type);

  /** LN Added */
  public Synset getSynset();
}