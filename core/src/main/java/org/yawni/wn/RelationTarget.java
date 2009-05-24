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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice.
 */
package org.yawni.wn;

import java.util.List;

/**
 * A <code>RelationTarget</code> is the <i>source</i> or <i>target</i> of a {@link Relation}.
 * The target of a <b>semantic</b> <code>RelationTarget</code> is a {@link Synset};
 * the target of a <b>lexical</b> <code>RelationTarget</code> is a {@link WordSense}.
 *
 * @see Relation
 * @see Synset
 * @see WordSense
 */
public interface RelationTarget extends Iterable<WordSense> {
  public POS getPOS();

  /**
   * Returns a description of the target.  For a <code>WordSense</code>, this is
   * it's lemma; for a <code>Synset</code>, it's the concatenated lemma's of
   * its <code>WordSense</code>s.
   */
  public String getDescription();

  /**
   * Returns a long description of the target.  This is its description,
   * appended by, if it exists, a dash and it's gloss.
   */
  public String getLongDescription();

  /**
   * Returns the outgoing <code>Relation</code>s from this target -- those
   * <code>Relation</code>s that have this object as their source.
   */
  public List<Relation> getRelations();

  /** Returns the outgoing <code>Relation</code>s of type {@code type}. */
  public List<Relation> getRelations(RelationType type);

  /** Returns the targets of the outgoing <code>Relation</code>s. */
  public List<RelationTarget> getTargets();

  /**
   * Returns the targets of the outgoing <code>Relation</code>s that have type
   * {@code type}.
   */
  public List<RelationTarget> getTargets(RelationType type);

  /** LN Added */
  public Synset getSynset();
}