/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;
import edu.brandeis.cs.steele.util.ArrayUtilities;
import java.util.*;

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
public class PointerType {
  // Flags for tagging a pointer type with the POS types it apples to.
  protected static final int N = 1;
  protected static final int V = 2;
  protected static final int ADJ = 4;
  protected static final int ADV = 8;
  protected static final int SAT_ADJ = 16;
  protected static final int LEXICAL = 32;

  protected static final POS[] CATS = {POS.NOUN, POS.VERB, POS.ADJ, POS.ADV, POS.SAT_ADJ};
  protected static final int[] POS_MASK = {N, V, ADJ, ADV, SAT_ADJ, LEXICAL};

  // All categories
  public static final PointerType ANTONYM = new PointerType("antonym", "!", N | V | ADJ | ADV | LEXICAL);
  public static final PointerType DOMAIN_OF_TOPIC = new PointerType("Domain of synset - TOPIC", ";c", N | V | ADJ | ADV); // LN fixed
  public static final PointerType MEMBER_OF_THIS_DOMAIN_TOPIC = new PointerType("Member of this domain - TOPIC", "-c", N | V | ADJ | ADV); // LN
  public static final PointerType DOMAIN_OF_REGION = new PointerType("Domain of synset - REGION", ";r", N | V | ADJ | ADV);
  public static final PointerType MEMBER_OF_THIS_DOMAIN_REGION = new PointerType("Member of this domain - REGION", "-r", N | V | ADJ | ADV); // LN
  public static final PointerType DOMAIN_OF_USAGE = new PointerType("Domain of synset - USAGE", ";u", N | V | ADJ | ADV);
  public static final PointerType MEMBER_OF_THIS_DOMAIN_USAGE = new PointerType("Member of this domain - USAGE", "-u", N | V | ADJ | ADV); // LN
  public static final PointerType DOMAIN_MEMBER = new PointerType("Domain Member", "-", N | V | ADJ | ADV);
  public static final PointerType DOMAIN = new PointerType("Domain", ";", N | V | ADJ | ADV);


  // Nouns and Verbs
  public static final PointerType HYPERNYM = new PointerType("hypernym", "@", N | V);
  public static final PointerType INSTANCE_HYPERNYM = new PointerType("instance hypernym", "@i", N | V); // LN
  public static final PointerType HYPONYM = new PointerType("hyponym", "~", N | V);
  public static final PointerType INSTANCE_HYPONYM = new PointerType("instance hyponym", "~i", N | V); // LN
  public static final PointerType DERIVATIONALLY_RELATED = new PointerType("derivationally related", "+", N | V);

  // Nouns and Adjectives
  public static final PointerType ATTRIBUTE = new PointerType("attribute", "=", N | ADJ);
  public static final PointerType SEE_ALSO = new PointerType("also see", "^", N | ADJ | LEXICAL);

  // Verbs
  public static final PointerType ENTAILMENT = new PointerType("entailment", "*", V);
  public static final PointerType CAUSE = new PointerType("cause", ">", V);
  public static final PointerType VERB_GROUP = new PointerType("verb group", "$", V);

  // Nouns
  public static final PointerType MEMBER_MERONYM = new PointerType("member meronym", "%m", N); // LN fixed
  public static final PointerType SUBSTANCE_MERONYM = new PointerType("substance meronym", "%s", N); // LN fixed
  public static final PointerType PART_MERONYM = new PointerType("part meronym", "%p", N); // LN fixed
  public static final PointerType MEMBER_HOLONYM = new PointerType("member holonym", "#m", N); // LN fixed
  public static final PointerType SUBSTANCE_HOLONYM = new PointerType("substance holonym", "#s", N); // LN fixed
  public static final PointerType PART_HOLONYM = new PointerType("part holonym", "#p", N); // LN fixed
  public static final PointerType MEMBER_OF_TOPIC_DOMAIN = new PointerType("Member of TOPIC domain", "-c", N);
  public static final PointerType MEMBER_OF_REGION_DOMAIN = new PointerType("Member of REGION domain", "-r", N);
  public static final PointerType MEMBER_OF_USAGE_DOMAIN = new PointerType("Member of USAGE domain", "-u", N);

  // Adjectives
  public static final PointerType SIMILAR_TO = new PointerType("similar", "&", ADJ);
  public static final PointerType PARTICIPLE_OF = new PointerType("participle of", "<", ADJ | LEXICAL);
  public static final PointerType PERTAINYM = new PointerType("pertainym", "\\", ADJ | LEXICAL);

  // Adverbs
  public static final PointerType DERIVED = new PointerType("derived from", "\\", ADV);	// from adjective

  /** A list of all <code>PointerType</code>s. */
  public static final PointerType[] TYPES = {
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
  };

  static {
    // seems to be 30
    //assert TYPES.length == 32 : "TYPES.length: "+TYPES.length+" "+Arrays.toString(TYPES);
  }

  public static final PointerType[] INDEX_ONLY = { DOMAIN_MEMBER, DOMAIN };

  static protected void setSymmetric(final PointerType a, final PointerType b) {
    a.symmetricType = b;
    b.symmetricType = a;
  }

  /**
   * Index-only pointer types are used for the sole purpose of parsing index file records.
   * They are not used to determine relationships between words.
   * @param pType
   * @return True if the pType is an index-only pointer type.  Otherwise, it is false.
   */
  public static boolean isIndexOnly(PointerType pType) {
    for (int i=0; i<INDEX_ONLY.length; ++i) {
      if (pType.getKey().equals(INDEX_ONLY[i].getKey())) {
        return true;
      }
    }
    return false;
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

  /** Return the <code>PointerType</code> whose key matches <var>key</var>.
   * @exception NoSuchElementException If <var>key</var> doesn't name any <code>PointerType</code>.
   */
  static PointerType parseKey(final String key) {
    for (int i = 0; i < TYPES.length; ++i) {
      final PointerType type = TYPES[i];
      if (type.key.equals(key)) {
        return type;
      }
    }
    for (int i = 0; i < INDEX_ONLY.length; ++i) {
      final PointerType type = INDEX_ONLY[i];
      if (type.key.equals(key)) {
        return type;
      }
    }
    throw new NoSuchElementException("unknown link type " + key);
  }

  /*
   * Instance Interface
   */
  protected final String label;
  protected final String key;
  protected final int flags;
  protected PointerType symmetricType;

  protected PointerType(final String label, final String key, final int flags) {
    this.label = label;
    this.key = key;
    this.flags = flags;
  }

  @Override public String toString() {
    return getLabel()+" "+getKey();
  }

  public String getLabel() {
    return label;
  }

  public String getKey() {
    return this.key;
  }

  public boolean appliesTo(final POS pos) {
    return (flags & POS_MASK[ArrayUtilities.indexOf(CATS, pos)]) != 0;
  }

  public boolean symmetricTo(PointerType type) {
    return symmetricType != null && symmetricType.equals(type);
  }
}
