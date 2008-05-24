/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

/** A <code>PointerTarget</code> is the <i>source</i> or <i>target</i> of a <code>Pointer</code>.
 * The target of a semantic <code>PointerTarget</code> is a <code>Synset</code>;
 * the target of a lexical <code>PointerTarget</code> is a <code>WordSense</code>.
 *
 * @see Pointer
 * @see Synset
 * @see WordSense
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public interface PointerTarget extends Iterable<WordSense> {
  public POS getPOS();

  /** Return a description of the target.  For a <code>WordSense</code>, this is
   * it's lemma; for a <code>Synset</code>, it's the concatenated lemma's of
   * its <code>WordSense</code>s.
   */
  public String getDescription();

  /** Return the long description of the target.  This is its description,
   * appended by, if it exists, a dash and it's gloss.
   */
  public String getLongDescription();

  /** Return the outgoing <code>Pointer</code>s from this target -- those
   * <code>Pointer</code>s that have this object as their source.
   */
  public Pointer[] getPointers();

  /** Return the outgoing <code>Pointer</code>s of type <var>type</var>. */
  public Pointer[] getPointers(PointerType type);

  /** Return the targets of the outgoing <code>Pointer</code>s. */
  public PointerTarget[] getTargets();

  /** Return the targets of the outgoing <code>Pointer</code>s that have type
   * <var>type</var>. 
   */
  public PointerTarget[] getTargets(PointerType type);

  /** LN Added */
  public Synset getSynset();
}
