/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.*;
import static edu.brandeis.cs.steele.wn.PointerTypeFlags.*;

/** Instances of this class enumerate the possible WordNet pointer types, and
 * are used to label <code>PointerType</code>s.  
 * Each <code>PointerType</code> carries additional information:
 * <ul>
 *   <li> a human-readable label </li>
 *   <li> an optional reflexive type that labels links pointing the opposite direction </li>
 *   <li> an encoding of parts-of-speech that it applies to </li>
 *   <li> a short string (lemma) that represents it in the dictionary files </li>
 * </ul>
 *
 * @see Pointer
 * @see POS
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public enum PointerType {
  // All parts of speech
  ANTONYM("antonym", "!", N | V | ADJ | ADV | LEXICAL),
  DOMAIN_OF_TOPIC("Domain of synset - TOPIC", ";c", N | V | ADJ | ADV),
  MEMBER_OF_THIS_DOMAIN_TOPIC("Member of this domain - TOPIC", "-c", N | V | ADJ | ADV),
  DOMAIN_OF_REGION("Domain of synset - REGION", ";r", N | V | ADJ | ADV),
  MEMBER_OF_THIS_DOMAIN_REGION("Member of this domain - REGION", "-r", N | V | ADJ | ADV),
  DOMAIN_OF_USAGE("Domain of synset - USAGE", ";u", N | V | ADJ | ADV),
  MEMBER_OF_THIS_DOMAIN_USAGE("Member of this domain - USAGE", "-u", N | V | ADJ | ADV),
  DOMAIN_MEMBER("Domain Member", "-", N | V | ADJ | ADV),
  DOMAIN("Domain", ";", N | V | ADJ | ADV),

  // Nouns and Verbs
  HYPERNYM("hypernym", "@", N | V),
  INSTANCE_HYPERNYM("instance hypernym", "@i", N | V),
  HYPONYM("hyponym", "~", N | V),
  INSTANCE_HYPONYM("instance hyponym", "~i", N | V),
  DERIVATIONALLY_RELATED("derivationally related", "+", N | V),

  // Nouns and Adjectives
  ATTRIBUTE("attribute", "=", N | ADJ),
  SEE_ALSO("also see", "^", N | ADJ | LEXICAL),

  // Verbs
  ENTAILMENT("entailment", "*", V),
  CAUSE("cause", ">", V),
  VERB_GROUP("verb group", "$", V),

  // Nouns
  MEMBER_MERONYM("member meronym", "%m", N),
  SUBSTANCE_MERONYM("substance meronym", "%s", N),
  PART_MERONYM("part meronym", "%p", N),
  MEMBER_HOLONYM("member holonym", "#m", N),
  SUBSTANCE_HOLONYM("substance holonym", "#s", N),
  PART_HOLONYM("part holonym", "#p", N),
  MEMBER_OF_TOPIC_DOMAIN("Member of TOPIC domain", "-c", N),
  MEMBER_OF_REGION_DOMAIN("Member of REGION domain", "-r", N),
  MEMBER_OF_USAGE_DOMAIN("Member of USAGE domain", "-u", N),

  // Adjectives
  SIMILAR_TO("similar", "&", ADJ),
  PARTICIPLE_OF("participle of", "<", ADJ | LEXICAL),
  PERTAINYM("pertainym", "\\", ADJ | LEXICAL),

  // Adverbs
  DERIVED("derived from", "\\", ADV);	// from adjective

  //OLD private static final POS[] CATS = {POS.NOUN, POS.VERB, POS.ADJ, POS.ADV, POS.SAT_ADJ};
  private static final int[] POS_MASK = {N, V, ADJ, ADV, SAT_ADJ, LEXICAL};


  /** A list of all <code>PointerType</code>s. */
  public static final EnumSet<PointerType> TYPES = EnumSet.of(
    ANTONYM, HYPERNYM, HYPONYM, ATTRIBUTE, SEE_ALSO,
    ENTAILMENT, CAUSE, VERB_GROUP,
    MEMBER_MERONYM, SUBSTANCE_MERONYM, PART_MERONYM,
    MEMBER_HOLONYM, SUBSTANCE_HOLONYM, PART_HOLONYM,
    SIMILAR_TO, PARTICIPLE_OF, PERTAINYM, DERIVED,
    DOMAIN_OF_TOPIC, MEMBER_OF_THIS_DOMAIN_TOPIC, DOMAIN_OF_REGION, DOMAIN_OF_USAGE, 
    MEMBER_OF_THIS_DOMAIN_REGION, MEMBER_OF_THIS_DOMAIN_USAGE,
    MEMBER_OF_TOPIC_DOMAIN, MEMBER_OF_REGION_DOMAIN, MEMBER_OF_USAGE_DOMAIN,
    DERIVATIONALLY_RELATED,
    INSTANCE_HYPERNYM, INSTANCE_HYPONYM
  );

  public static final Set<PointerType> INDEX_ONLY = EnumSet.of(DOMAIN_MEMBER, DOMAIN);

  static {
    assert EnumSet.complementOf(TYPES).equals(INDEX_ONLY);
  }

  static private void setSymmetric(final PointerType a, final PointerType b) {
    a.symmetricType = b;
    b.symmetricType = a;
  }

  /**
   * Index-only pointer types are used for the sole purpose of parsing index file records.
   * They are not used to determine relationships between words.
   * @param pType
   * @return True if the pType is an index-only pointer type.  Otherwise, it is false.
   */
  public static boolean isIndexOnly(final PointerType pType) {
    return INDEX_ONLY.contains(pType);
  }

  static {
    setSymmetric(ANTONYM, ANTONYM);
    setSymmetric(HYPERNYM, HYPONYM);
    setSymmetric(INSTANCE_HYPERNYM, INSTANCE_HYPONYM);
    setSymmetric(MEMBER_MERONYM, MEMBER_HOLONYM);
    setSymmetric(SUBSTANCE_MERONYM, SUBSTANCE_HOLONYM);
    setSymmetric(PART_MERONYM, PART_HOLONYM);
    setSymmetric(SIMILAR_TO, SIMILAR_TO);
    setSymmetric(ATTRIBUTE, ATTRIBUTE);
    setSymmetric(DERIVATIONALLY_RELATED, DERIVATIONALLY_RELATED);
    setSymmetric(DOMAIN_OF_TOPIC, MEMBER_OF_TOPIC_DOMAIN);
    setSymmetric(DOMAIN_OF_REGION, MEMBER_OF_REGION_DOMAIN);
    setSymmetric(DOMAIN_OF_USAGE, MEMBER_OF_USAGE_DOMAIN);
    setSymmetric(DOMAIN_OF_TOPIC, DOMAIN_MEMBER);
    setSymmetric(DOMAIN_OF_REGION, DOMAIN_MEMBER);
    setSymmetric(DOMAIN_OF_USAGE, DOMAIN_MEMBER);
    setSymmetric(VERB_GROUP, VERB_GROUP);
  }

  private static final PointerType[] VALUES = values();

  static PointerType fromOrdinal(final byte ordinal) {
    return VALUES[ordinal];
  }

  /** Return the <code>PointerType</code> whose key matches <var>key</var>.
   * @exception NoSuchElementException If <var>key</var> doesn't name any <code>PointerType</code>.
   */
  public static PointerType parseKey(final CharSequence key) {
    for (final PointerType pType : VALUES) {
      if (pType.key.contentEquals(key)) {
        return pType;
      }
    }
    throw new NoSuchElementException("unknown link type " + key);
  }

  /*
   * Instance Interface
   */
  private final String label;
  private final String key;
  private final int flags;
  private final String toString;
  private PointerType symmetricType;

  PointerType(final String label, final String key, final int flags) {
    this.label = label;
    this.key = key;
    this.flags = flags;
    this.toString = getLabel()+" "+getKey();
  }

  @Override public String toString() {
    return toString;
  }

  public String getLabel() {
    return label;
  }

  public String getKey() {
    return this.key;
  }

  public boolean appliesTo(final POS pos) {
    //OLD return (flags & POS_MASK[ArrayUtilities.indexOf(CATS, pos)]) != 0;
    return (flags & POS_MASK[pos.ordinal()]) != 0;
  }

  public boolean symmetricTo(final PointerType type) {
    return symmetricType != null && symmetricType.equals(type);
  }
}

/** 
 * Flags for tagging a pointer type with the POS types it apples to. 
 * Separate class to allow PointerType enum constructor to reference it.
 */
class PointerTypeFlags {
  static final int N = 1;
  static final int V = 2;
  static final int ADJ = 4;
  static final int ADV = 8;
  static final int SAT_ADJ = 16;
  static final int LEXICAL = 32;
}
