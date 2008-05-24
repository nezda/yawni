/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.*;
import static edu.brandeis.cs.steele.wn.PointerTypeFlag.*;

/** Instances of this class enumerate the possible WordNet pointer types, and
 * are used to label {@link Pointer}s.  
 * Each <code>PointerType</code> carries additional information:
 * <ul>
 *   <li> a human-readable label </li>
 *   <li> an optional symmetric (i.e. reflexive) type that labels links pointing the opposite direction </li>
 *   <li> an encoding of parts-of-speech that it applies to </li>
 *   <li> a short string (lemma) that represents it in the dictionary files </li>
 * </ul>
 *
 * @see <a href="http://wordnet.princeton.edu/man/wnsearch.3WN#sect4>http://wordnet.princeton.edu/man/wnsearch.3WN#sect4</a>
 * @see <a href="http://wordnet.princeton.edu/man/wngloss.7WN.html#sect4">Glossary of WordNet Terms</a>
 * @see Pointer
 * @see POS
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public enum PointerType {
  // consider Unicde ellipsis: "…" instead of "..."

  // Nouns and Verbs 
  HYPERNYM("hypernym", "@", N | V, "Hypernyms (%s is a kind of ...)", "Hypernyms (%s is one way to ...)"),
  INSTANCE_HYPERNYM("instance hypernym", "@i", N | V, "Instance Hypernyms (%s is an instance of ...)"),
  HYPONYM("hyponym", "~", N | V, "Hyponyms (... is a kind of %s)", "Troponyms (... are particular ways to %s)"),
  INSTANCE_HYPONYM("instance hyponym", "~i", N | V, "Instance Hyponyms (... is an instance of %s)"),
  DERIVATIONALLY_RELATED("derivationally related", "+", N | V, "Derivationally related forms"),

  // Nouns and Adjectives
  ATTRIBUTE("attribute", "=", N | ADJ, "Attribute (%s is a value of ...)"),
  SEE_ALSO("also see", "^", N | ADJ | LEXICAL),

  // Verbs
  ENTAILMENT("entailment", "*", V, "%s entails doing ..."),
  CAUSE("cause", ">", V, null, "%s causes ..."),
  VERB_GROUP("verb group", "$", V),

  // Nouns
  /** 
   * A word that names a part of a larger whole, aka "part name".
   * Pure-virtual PointerType.
   * @see PointerType#MEMBER_MERONYM
   * @see PointerType#SUBSTANCE_MERONYM
   * @see PointerType#PART_MERONYM
   */
  MERONYM("meronym", "%" /* non-existent */, N),
  MEMBER_MERONYM("member meronym", "%m", N, "Member Meronyms (... are members of %s)"),
  PART_MERONYM("part meronym", "%p", N, "Part Meronyms (... are parts of %s)"),
  SUBSTANCE_MERONYM("substance meronym", "%s", N, "Substance Meronyms (... are substances of %s)"),

  /** 
   * A word that names the whole of which a given word is a part.
   * Pure-virtual PointerType.
   * @see PointerType#MEMBER_HOLONYM
   * @see PointerType#SUBSTANCE_HOLONYM
   * @see PointerType#PART_HOLONYM
   */
  HOLONYM("holonym", "#" /* non-existent up */, N),
  MEMBER_HOLONYM("member holonym", "#m", N, "Member Holonyms (%s is a member of ...)"),
  SUBSTANCE_HOLONYM("substance holonym", "#s", N, "Substance Holonyms (%s is a substance of ...)"),
  PART_HOLONYM("part holonym", "#p", N, "Part Holonyms (%s is a part of ...)"),
  
  MEMBER_OF_TOPIC_DOMAIN("Member of TOPIC domain", "-c", N), // topic term
  MEMBER_OF_REGION_DOMAIN("Member of REGION domain", "-r", N), // regional term
  MEMBER_OF_USAGE_DOMAIN("Member of USAGE domain", "-u", N), // ?

  // Adjectives
  SIMILAR_TO("similar to", "&", ADJ),
  PARTICIPLE_OF("participle of", "<", ADJ | LEXICAL),
  PERTAINYM("pertainym", "\\", ADJ | LEXICAL, "... are nouns related to %s"),

  // Adverbs
  DERIVED("derived from", "\\", ADV), // from adjective

  // All parts of speech
  ANTONYM("antonym", "!", N | V | ADJ | ADV | LEXICAL, "Antonyms (... is the opposite of %s)"),

  DOMAIN_OF_TOPIC("Domain of synset - TOPIC", ";c", N | V | ADJ | ADV), // a domain
  DOMAIN_OF_REGION("Domain of synset - REGION", ";r", N | V | ADJ | ADV), // a region
  DOMAIN_OF_USAGE("Domain of synset - USAGE", ";u", N | V | ADJ | ADV), // ?
  
  /** 
   * Pure-virtual PointerType. 
   * @see PointerType#MEMBER_OF_TOPIC_DOMAIN
   * @see PointerType#MEMBER_OF_REGION_DOMAIN
   * @see PointerType#MEMBER_OF_USAGE_DOMAIN
   */
  DOMAIN_MEMBER("Domain Member", "-", N | V | ADJ | ADV),

  /** 
   * Pure-virtual PointerType.
   * @see PointerType#DOMAIN_OF_TOPIC
   * @see PointerType#DOMAIN_OF_REGION
   * @see PointerType#DOMAIN_OF_USAGE
   */
  DOMAIN("Domain", ";", N | V | ADJ | ADV);

  private static final int[] POS_MASK = {N, V, ADJ, ADV, SAT_ADJ, LEXICAL};

  /** A list of all <code>PointerType</code>s. */
  public static final EnumSet<PointerType> TYPES = EnumSet.of(
    ANTONYM, HYPERNYM, HYPONYM, ATTRIBUTE, SEE_ALSO,
    ENTAILMENT, CAUSE, VERB_GROUP,
    MEMBER_MERONYM, SUBSTANCE_MERONYM, PART_MERONYM,
    MEMBER_HOLONYM, SUBSTANCE_HOLONYM, PART_HOLONYM,
    SIMILAR_TO, PARTICIPLE_OF, PERTAINYM, DERIVED,
    DOMAIN_OF_TOPIC, DOMAIN_OF_USAGE, DOMAIN_OF_REGION, 
    MEMBER_OF_TOPIC_DOMAIN, MEMBER_OF_REGION_DOMAIN, MEMBER_OF_USAGE_DOMAIN,
    DERIVATIONALLY_RELATED,
    INSTANCE_HYPERNYM, INSTANCE_HYPONYM
  );

  //XXX this seems to indicate DOMAIN implies DOMAIN_PART
  public static final Set<PointerType> INDEX_ONLY = EnumSet.of(DOMAIN_MEMBER, DOMAIN, HOLONYM, MERONYM);

  static {
    assert EnumSet.complementOf(TYPES).equals(INDEX_ONLY);
  }

  static private void setSymmetric(final PointerType a, final PointerType b) {
    a.symmetricType = b;
    b.symmetricType = a;
  }

  /**
   * A "pure-virtual" concept (ie one that cannot be directly instantiated).
   * Index-only pointer types are used only for parsing index file records.
   * <code>isIndexOnly</code> <code>PointerType</code>s are not used to determine relationships between words.
   * @param pointerType
   * @return <code>true</code> if the <code>pointerType</code> is an index-only pointer type, otherwise <code>false</code>.
   */
  public static boolean isIndexOnly(final PointerType pointerType) {
    return INDEX_ONLY.contains(pointerType);
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

    /**
     * Some PointerTypes are "abstract/virtual/meta", though most are concrete.<br>
     * "virtual" means has superTypes and/or subTypes.<br>
     * Compare to "concrete" (isolated) and "pure virtual" (incomplete) types.<br>
     * It does not make sense to search for a pure-virtual type.<br>
     * <br>
     * virtual:
     * <ul>
     *   <li> Hyponym, Instance </li>
     *   <li> Hypernym, Instance </li>
     * </ul>
     * <br>
     * pure-virtual:
     * <ul>
     *   <li> Holonym:: Part, Member, Substance </li>
     *   <li> Meronym:: Part, Member, Substance </li>
     *   
     *   <li> Domain:: {Member, Domain} X {Topic, Region, Usage} </li>
     * </ul>
     * 
     * Adjective - not sure how this fits in
     *   Similar To
     *
     *   Also see -- verb only ?
     */
    
    /**
     * Usage of types, subTypes and superTypes:
     * <ul>
     *   <li> if superTypes exist, search them, then current type </li>
     *   <li> if current type exists, search it, then if subTypes exist, search them </li>
     * </ul>
     *
     * Example 1: 
     * { George Bush } --instance hyper--> { President of the United States }
     * George Bush, <i>the instance</i>, does not particpate in any other hyper/hypo relationships.  
     * That said, how does he get linked to hypernym { person } ?
     *
     * MORE GENERAL:  { President of the United States } --hyper--> { head of state } --hyper-->
     *                { representative } ... hyper--> ... { person } ...
     * MORE_SPECIFIC: { President of the United States } --instance hypo--> { George Bush, ... }
     *
     * Axioms
     * - if ∃ x, y s.t. InstanceHyponym(x, y), then InstanceHypernym(y, z) (symmetric)
     *   e.g. x=GW, y=PofUS
     *   - "inflection point" ? InstanceHyponym is root-tip (e.g. opposite of leaf) in ontology,
     *     while InstanceHypernym is leaf (living tree-sense, top/edgemost)
     * - if ∃ x, y s.t. InstanceHypernym(x, y), then ∃ z s.t. Hypernym(y, z)
     *   e.g. x=GW, y=PofUS, z=person
     *   - "inheritance" ?
     */

    // e.g. "Bill Clinton" has instance hypernym "president"#2 
    // which in turn has normal hypernyms
    HYPERNYM.superTypes = Arrays.asList(INSTANCE_HYPERNYM);
    // e.g. "president"#2 (* the Synset) has instance hyponyms ("George Washington", ...) AND
    // normal hyponyms ("chief of state", ...).  Note that while "president"#2 also
    // has normal hyponyms, "President of the United States" does NOT since
    // it is more lexically specified.
    // FIXME this example seems to show a bad, unneeded asymmetry
    HYPONYM.subTypes = Arrays.asList(INSTANCE_HYPONYM);

    MERONYM.subTypes = Arrays.asList(MEMBER_MERONYM, PART_MERONYM, SUBSTANCE_MERONYM);
    // don't assign superTypes since MERONYM is pure-virtual
    HOLONYM.subTypes = Arrays.asList(MEMBER_HOLONYM, PART_HOLONYM, SUBSTANCE_HOLONYM);
    // don't assign superTypes since HOLONYM is pure-virtual

    DOMAIN.subTypes = Arrays.asList(DOMAIN_OF_TOPIC, DOMAIN_OF_REGION, DOMAIN_OF_USAGE);
    // don't assign superTypes since DOMAIN is pure-virtual
  }

  private static final PointerType[] VALUES = values();

  static PointerType fromOrdinal(final byte ordinal) {
    return VALUES[ordinal];
  }

  /** 
   * @return the <code>PointerType</code> whose key matches <var>key</var>.
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
  private final String longNounLabel;
  private final String longVerbLabel;
  private final String key;
  private final int flags;
  private final String toString;
  private PointerType symmetricType;
  private List<PointerType> subTypes;
  private List<PointerType> superTypes;

  PointerType(final String label, final String key, final int flags) {
    this(label, key, flags, null, null);
  }
  
  PointerType(final String label, final String key, final int flags, final String longNounLabel) {
    this(label, key, flags, longNounLabel, null);
  }

  PointerType(final String label, final String key, final int flags, final String longNounLabel, final String longVerbLabel) {
    this.label = label;
    this.key = key;
    this.flags = flags;
    this.toString = getLabel()+" "+getKey();
    if (longNounLabel != null) {
      this.longNounLabel = longNounLabel;
    } else {
      this.longNounLabel = label;
    }
    if (longVerbLabel != null) {
      this.longVerbLabel = longVerbLabel;
    } else {
      if (longNounLabel != null) {
        this.longVerbLabel = longNounLabel;
      } else {
        this.longVerbLabel = label;
      }
    }
    this.superTypes = Collections.emptyList();
    this.subTypes = Collections.emptyList();
    //XXX System.err.println(this+" longNounLabel: "+this.longNounLabel+" longVerbLabel: "+this.longVerbLabel+" label: "+this.label);
  }

  @Override public String toString() {
    return toString;
  }

  /**
   * @return human-readable label.
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return labels with <code>"%s"</code> variables which can be substituted
   * for to create textual content of WordNet interfaces.
   */
  public String getFormatLabel(final POS pos) {
    switch(pos) {
      case NOUN: return longNounLabel;
      case VERB: return longVerbLabel;
      default: return longNounLabel;
    }
  }

  public String getKey() {
    return this.key;
  }

  /**
   * Some <code>PointerType</code>s only apply to certain {@link POS}.
   */
  public boolean appliesTo(final POS pos) {
    return (flags & POS_MASK[pos.ordinal()]) != 0;
  }

  /**
   * <code>type</code> is the opposite concept of <code>this</code>.
   * For example <code>{@link PointerType#HYPERNYM}.symmetricTo({@link PointerType#HYPONYM}<code>).
   */
  public boolean symmetricTo(final PointerType type) {
    return symmetricType != null && symmetricType.equals(type);
  }

  public List<PointerType> getSuperTypes() {
    return this.superTypes;
  }
  
  public List<PointerType> getSubTypes() {
    return this.subTypes;
  }
}

/** 
 * Flags for tagging a pointer type with the POS types it apples to. 
 * Separate class to allow PointerType enum constructor to reference it.
 */
class PointerTypeFlag {
  static final int N = 1;
  static final int V = 2;
  static final int ADJ = 4;
  static final int ADV = 8;
  static final int SAT_ADJ = 16;
  static final int LEXICAL = 32;
}
