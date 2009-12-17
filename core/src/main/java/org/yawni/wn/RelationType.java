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

import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.yawni.util.ImmutableList;
import static org.yawni.wn.RelationTypeFlag.*;

/**
 * Instances of this class enumerate the possible WordNet relation types, and
 * are used to label {@link Relation}s.
 * Each {@code RelationType} carries additional information including:
 * <ul>
 *   <li> a human-readable label </li>
 *   <li> an optional symmetric (i.e., reflexive) type that labels links pointing the opposite direction </li>
 *   <li> an encoding of parts-of-speech that it applies to </li>
 *   <li> a short string that represents it in the dictionary files </li>
 * </ul>
 *
 * <p> Note this class used to be called {@code PointerType}.
 * 
 * <p> Relevant C code is in {@code include/wn.h}, {@code src/wnglobal.c}
 *
 * @see <a href="http://wordnet.princeton.edu/wordnet/man/wnsearch.3WN.html#sect4">http://wordnet.princeton.edu/wordnet/man/wnsearch.3WN.html#sect4</a>
 * @see <a href="http://wordnet.princeton.edu/man/wngloss.7WN.html#sect4">Glossary of WordNet Terms</a>
 * @see Relation
 * @see POS
 * @see <a href="http://wordnet.princeton.edu/man/wnsearch.3WN.html#sect4">WordNet Searches</a>
 */
public enum RelationType {
  // consider Unicde ellipsis: "…" instead of "..."

  // Nouns and Verbs
  /** "a word that is <em>more generic</em> than a given word" */
  HYPERNYM("hypernym", "@", 2, N | V, "Hypernyms (%s is a kind of ...)", "Hypernyms (%s is one way to ...)"),
  /** aka "instance of" or just "instance" */
  INSTANCE_HYPERNYM("instance hypernym", "@i", 38, N, "Instance Hypernyms (%s is an instance of ...)"),
  /**
   * "a word that is <em>more specific</em> than a given word".
   * analagous to "troponym" for verbs.
   */
  HYPONYM("hyponym", "~", 3, N | V, "Hyponyms (... is a kind of %s)", "Troponyms (... are particular ways to %s)"),
  /** aka "instances" / "has instance" */
  INSTANCE_HYPONYM("instance hyponym", "~i", 39, N, "Instance Hyponyms (... is an instance of %s)"),
  /** aka "derivation", "nominalization" */
  DERIVATIONALLY_RELATED("derivationally related", "+", 20, N | V | LEXICAL, "Derivationally related forms"),

  // Nouns and Adjectives

  ATTRIBUTE("attribute", "=", 18, N | ADJ, "Attribute (%s is a value of ...)"),
  /**
   * Lexical variant shows phrasal verbs (e.g., "turn" → "turn around").
   * aka "also see"
   */
  SEE_ALSO("also see", "^", 16, N | V | ADJ),

  // Verbs
  
  ENTAILMENT("entailment", "*", 4, V, "%s entails doing ..."),
  /** aka "'cause to'" */
  CAUSE("cause", ">", 14, V, null, "%s causes ..."),
  /**
   * Verb senses that are similar in meaning and have been manually grouped together.
   * Lexical examples: V"bear"#4 → V"bear"#12 and V"bear"#12 → V"bear"#4
   */
  VERB_GROUP("verb group", "$", 19, V),

  // Nouns
  
  /**
   * A word that names a part of a larger whole, aka "part name".<br>
   * Pure-virtual RelationType.
   * @see RelationType#MEMBER_MERONYM
   * @see RelationType#SUBSTANCE_MERONYM
   * @see RelationType#PART_MERONYM
   */
  MERONYM("meronym", "%" /* non-existent */, 12, N),
  /** aka "is member" */ // HASMEMBERPTR
  MEMBER_MERONYM("member meronym", "%m", 6, N, "Member Meronyms (... are members of %s)"),
  /** aka "is stuff" */ // HASSTUFFPTR
  SUBSTANCE_MERONYM("substance meronym", "%s", 7, N, "Substance Meronyms (... are substances of %s)"),
  /** aka "is part" */ // HASPARTPTR
  PART_MERONYM("part meronym", "%p", 8, N, "Part Meronyms (... are parts of %s)"),

  /**
   * A word that names the whole of which a given word is a part.<br>
   * Pure-virtual RelationType.
   * @see RelationType#MEMBER_HOLONYM
   * @see RelationType#SUBSTANCE_HOLONYM
   * @see RelationType#PART_HOLONYM
   */
  HOLONYM("holonym", "#" /* non-existent */, 13, N),
  /** aka "has member" */ // ISMEMBERPTR
  MEMBER_HOLONYM("member holonym", "#m", 9, N, "Member Holonyms (%s is a member of ...)"),
  /** aka "has stuff" */ // ISSTUFFPTR
  SUBSTANCE_HOLONYM("substance holonym", "#s", 10, N, "Substance Holonyms (%s is a substance of ...)"),
  /** aka "has part" */ // ISPARTPTR
  PART_HOLONYM("part holonym", "#p", 11, N, "Part Holonyms (%s is a part of ...)"),

  // domain terms
  /** aka "topic term" */
  MEMBER_OF_TOPIC_DOMAIN("Member of TOPIC domain", "-c", 35, N),
  /** aka "usage term" */
  MEMBER_OF_USAGE_DOMAIN("Member of USAGE domain", "-u", 36, N),
  /** aka "regional term" */
  MEMBER_OF_REGION_DOMAIN("Member of REGION domain", "-r", 37, N),

  // Adjectives
  /** 
   * Connects adjective 'head' sense and its 'satellite' senses which
   * are similar in meaning to it (usually specializations).  Only the 'head sense'
   * has a 'direct antonym', and the 'satellite' senses have 'indirect
   * antonyms' through it.
   * @see <a href="http://wordnet.princeton.edu/man/wngloss.7WN.html#sect3">http://wordnet.princeton.edu/man/wngloss.7WN.html#sect3</a>
   * @see POS#SAT_ADJ
   */
  SIMILAR_TO("similar to", "&", 5, ADJ),
  /** adjective derived from a verb. */
  PARTICIPLE_OF("participle of", "<", 15, ADJ | LEXICAL),
  /** "a relational adjective." aka "pertains to noun (or another pertainym)".  do not have antonyms */
  PERTAINYM("pertainym", "\\", 17, ADJ | LEXICAL, "... are nouns related to %s"),

  // Adverbs
  
  /** aka "derived from adjective" */
  DERIVED("derived from", "\\", 17, ADV),

  // All parts of speech
  
  /** opposite word */
  ANTONYM("antonym", "!", 1, N | V | ADJ | ADV | LEXICAL, "Antonyms (... is the opposite of %s)"),

  // 'domains'
  /** aka "a topic/domain" */
  DOMAIN_OF_TOPIC("Domain of synset - TOPIC", ";c", 32, N | V | ADJ | ADV),
  /**
   * Frequently lexical.
   * aka "a usage type"
   */
  DOMAIN_OF_USAGE("Domain of synset - USAGE", ";u", 33, N | V | ADJ | ADV),
  /** aka "a region" */
  DOMAIN_OF_REGION("Domain of synset - REGION", ";r", 34, N | V | ADJ | ADV),

  /**
   * aka "class"<br>
   * Pure-virtual RelationType.
   * @see RelationType#MEMBER_OF_TOPIC_DOMAIN
   * @see RelationType#MEMBER_OF_REGION_DOMAIN
   * @see RelationType#MEMBER_OF_USAGE_DOMAIN
   */
  DOMAIN_MEMBER("Domain Member", "-", 22, N | V | ADJ | ADV),

  /**
   * aka "classification"<br>
   * Pure-virtual RelationType.
   * @see RelationType#DOMAIN_OF_TOPIC
   * @see RelationType#DOMAIN_OF_REGION
   * @see RelationType#DOMAIN_OF_USAGE
   */
  DOMAIN("Domain", ";", 21, N | V | ADJ | ADV);

  private static final int[] POS_MASK = {N, V, ADJ, ADV, SAT_ADJ, LEXICAL};

  /**
   * A list of all {@code RelationType}s.
   * Don't want to export this mutable, easily derived information.
   * @see RelationType#values()
   */
  private static final EnumSet<RelationType> TYPES = EnumSet.of(
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
  //XXX SAT_ADJ seems to be an index-only POS
  private static final Set<RelationType> INDEX_ONLY = EnumSet.of(DOMAIN_MEMBER, DOMAIN, HOLONYM, MERONYM);

  static {
    // checks for completeness of these 2 lists (TYPES and INDEX_ONLY = all the types)
    assert EnumSet.complementOf(TYPES).equals(INDEX_ONLY);
  }

  /**
   * A "pure-virtual" concept (i.e., one that cannot be directly instantiated).
   * Index-only relation types are used only for parsing index file records.
   * {@code isIndexOnly} {@code RelationType}s are not used to determine relationships between words.
   * @param relationType
   * @return {@code true} if the {@code relationType} is an index-only relation type, otherwise {@code false}.
   */
  public static boolean isIndexOnly(final RelationType relationType) {
    return INDEX_ONLY.contains(relationType);
  }

  /**
   * i.e., {@code HYPERNYM.isSymmetricTo(HYPONYM)}
   */
  private static void setSymmetric(final RelationType a, final RelationType b) {
    a.symmetricType = b;
    b.symmetricType = a;
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
     * Some {@code RelationType}s are "abstract/virtual/meta", though most are concrete.<br>
     * "Virtual" means has super-types and/or sub-types.<br>
     * Compare to "concrete" (isolated) and "pure virtual" (incomplete) types.<br>
     * It does not make sense to search for a pure-virtual type.
     * <h4> Virtual:</h4>
     * <ul>
     *   <li> Hyponym, Instance </li>
     *   <li> Hypernym, Instance </li>
     * </ul>
     * <h4> Pure-virtual:</h4>
     * <ul>
     *   <li> Holonym:: Part, Member, Substance </li>
     *   <li> Meronym:: Part, Member, Substance </li>
     *
     *   <li> Domain:: { Member, Domain } ⨯ { Topic, Region, Usage } </li>
     * </ul>
     *
     * Adjective - not sure how these fit in
     *   Similar To
     *
     *   Also see -- verb only ?
     *
     * <p>
     * Usage of types, {@code subTypes}, and {@code superTypes}:
     * <ul>
     *   <li> if {@code superTypes} exist, search them, then search current type </li>
     *   <li> if current type exists, search it, then if {@code subTypes} exist, search them </li>
     * </ul>
     *
     * Notation:
     *   { X } --has relation instance--> { Y }
     *   should be read "Synset X has relation instance to Synset Y
     *
     * Example 1:
     * { Bill Clinton } --instance hypernym--> { President of the United States }
     * Bill Clinton, <em>the instance</em>, does not particpate in any other hypernym/hyponym relationships.
     * That said, how does he get linked to hypernym { person } ?
     *
     * MORE GENERAL:  { President of the United States } --hypernym--> { head of state } --hypernym-->
     *                { representative } ... --hypernym--> ... { person } ...
     * MORE_SPECIFIC: { President of the United States } --instance hyponym--> { Bill Clinton, ... }
     *
     * Axioms
     * - if ∃ x, y s.t. InstanceHyponym(x, y), then InstanceHypernym(y, x) (symmetric)
     *   e.g., x=BC, y=PofUS
     *   - "inflection point" ? InstanceHyponym is root-tip (e.g., opposite of leaf) in ontology,
     *     while InstanceHypernym is leaf (living tree-sense, top/edgemost)
     * - if ∃ x, y s.t. InstanceHypernym(x, y), then ∃ z s.t. Hypernym(y, z)
     *   e.g., x=BC, y=PofUS, z=person
     *   - "inheritance" ?
     */

    /**
     * e.g., { Bill Clinton } (* the Synset) --instance hypernym--> { President of the United States }
     * which in turn has (normal) hypernyms
     */
    HYPERNYM.superTypes = ImmutableList.of(INSTANCE_HYPERNYM);
    INSTANCE_HYPERNYM.superTypes = ImmutableList.of(HYPERNYM);
//    INSTANCE_HYPERNYM.subTypes = ImmutableList.of(HYPERNYM);
    /**
     * e.g., { President of the United States } (* the Synset) --instance hyponyms--> ({ Bill Clinton }, ...) AND
     * (normal) hyponyms ({ chief of state }, ...).  Note that while { President of the United States } also
     * has (normal) hyponyms, { President of the United States } does NOT since
     * it is more lexically specified.
     * FIXME this example seems to show a bad, unneeded asymmetry
     */
    HYPONYM.subTypes = ImmutableList.of(INSTANCE_HYPONYM);
//    HYPONYM.superTypes = ImmutableList.of(INSTANCE_HYPONYM);

//    MERONYM.subTypes = ImmutableList.of(MEMBER_MERONYM, PART_MERONYM, SUBSTANCE_MERONYM);
    // don't assign superTypes since MERONYM is pure-virtual
//    HOLONYM.subTypes = ImmutableList.of(MEMBER_HOLONYM, PART_HOLONYM, SUBSTANCE_HOLONYM);
    // don't assign superTypes since HOLONYM is pure-virtual

//    DOMAIN.subTypes = ImmutableList.of(DOMAIN_OF_TOPIC, DOMAIN_OF_REGION, DOMAIN_OF_USAGE);
    // don't assign superTypes since DOMAIN is pure-virtual

    //TODO check sanity conditions
    //- type should never be its own supertype
    //- type should never be its own subtype
  }

  private static final RelationType[] VALUES = values();

  static RelationType fromOrdinal(final byte ordinal) {
    return VALUES[ordinal];
  }

  /**
   * @return the {@code RelationType} whose key matches {@code key}.
   * @throws NoSuchElementException If {@code key} doesn't name any {@code RelationType}.
   */
  public static RelationType parseKey(final CharSequence key) {
    for (final RelationType pType : VALUES) {
      if (pType.key.contentEquals(key)) {
        return pType;
      }
    }
    throw new NoSuchElementException("unknown link type " + key);
  }

  //
  // Instance Interface
  //
  private final String label;
  private final String longNounLabel;
  private final String longVerbLabel;
  private final String key;
  private final int value;
  private final int flags;
  private final String toString;
  private RelationType symmetricType;
  // experimental fields
  ImmutableList<RelationType> subTypes;
  private ImmutableList<RelationType> superTypes;

  RelationType(final String label, final String key, final int value, final int flags) {
    this(label, key, value, flags, null, null);
  }

  RelationType(final String label, final String key, final int value, final int flags, final String longNounLabel) {
    this(label, key, value, flags, longNounLabel, null);
  }

  RelationType(final String label, final String key, final int value, final int flags, final String longNounLabel, final String longVerbLabel) {
    this.label = label;
    this.key = key;
    this.value = value;
    this.flags = flags;
    this.toString = getLabel() + " " + getKey();
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
    this.superTypes = ImmutableList.of();
    this.subTypes = ImmutableList.of();
    //XXX System.err.println(this+" longNounLabel: "+this.longNounLabel+" longVerbLabel: "+this.longVerbLabel+" label: "+this.label);
  }

  @Override
  public String toString() {
    return toString;
  }

  /**
   * @return human-readable label, e.g. {@code RelationType.HYPERNYM.getLabel() == "hypernym"}
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return labels with {@code "%s"} variables which can be substituted
   * for to create textual content of WordNet interfaces.
   */
  public String getFormatLabel(final POS pos) {
    switch (pos) {
      case NOUN: return longNounLabel;
      case VERB: return longVerbLabel;
      default: return longNounLabel;
    }
  }

  public String getKey() {
    return this.key;
  }

  /**
   * Some {@code RelationType}s only apply to certain {@link POS}.
   */
  public boolean appliesTo(final POS pos) {
    return (flags & POS_MASK[pos.ordinal()]) != 0;
  }

  boolean isLexical() {
    return (flags & LEXICAL) != 0;
  }

  boolean isSemantic() {
    return ! isLexical();
  }

  /**
   * {@code type} is the opposite concept of {@code this}.
   * For example <code>{@link RelationType#HYPERNYM}.isSymmetricTo({@link RelationType#HYPONYM}<code>).
   * {@code isInverseOf} might've been a better name.
   */
  public boolean isSymmetricTo(final RelationType type) {
    return symmetricType != null && symmetricType.equals(type);
  }

  public List<RelationType> getSuperTypes() {
    return this.superTypes;
  }

  public List<RelationType> getSubTypes() {
    return this.subTypes;
  }
} // end enum RelationType

/**
 * Flags for tagging a relation type with the POS types it apples to.
 * Separate class to allow RelationType enum constructor to reference it.
 */
class RelationTypeFlag {
  static final int N = 1;
  static final int V = 2;
  static final int ADJ = 4;
  static final int ADV = 8;
  static final int SAT_ADJ = 16;
  /**
   * Special case indicator for lexical relations (those connecting specific {@code WordSense}s)
   * rather than the usual semantic relations which connect {@code Synset}s.
   */
  static final int LEXICAL = 32;
} // end class RelationTypeFlag