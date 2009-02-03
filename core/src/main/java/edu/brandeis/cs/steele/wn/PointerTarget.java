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
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

/** A <code>PointerTarget</code> is the <i>source</i> or <i>target</i> of a {@link Pointer}.
 * The target of a <b>semantic</b> <code>PointerTarget</code> is a {@link Synset};
 * the target of a <b>lexical</b> <code>PointerTarget</code> is a {@link WordSense}.
 *
 * @see Pointer
 * @see Synset
 * @see WordSense
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public interface PointerTarget extends Iterable<WordSense> {
  public POS getPOS();

  /** Returns a description of the target.  For a <code>WordSense</code>, this is
   * it's lemma; for a <code>Synset</code>, it's the concatenated lemma's of
   * its <code>WordSense</code>s.
   */
  public String getDescription();

  /** Returns a long description of the target.  This is its description,
   * appended by, if it exists, a dash and it's gloss.
   */
  public String getLongDescription();

  /** Returns the outgoing <code>Pointer</code>s from this target -- those
   * <code>Pointer</code>s that have this object as their source.
   */
  public Pointer[] getPointers();

  /** Returns the outgoing <code>Pointer</code>s of type <var>type</var>. */
  public Pointer[] getPointers(PointerType type);

  /** Returns the targets of the outgoing <code>Pointer</code>s. */
  public PointerTarget[] getTargets();

  /** Returns the targets of the outgoing <code>Pointer</code>s that have type
   * <var>type</var>.
   */
  public PointerTarget[] getTargets(PointerType type);

  /** LN Added */
  public Synset getSynset();
}
