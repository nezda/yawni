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

import java.util.HashMap;
import java.util.Map;
import static org.yawni.wn.POS.*;

/**
 * Represents the contents of (WordNet 3.0) lexnames since it is
 * "optional".  These are the WordNet lexicographers classes, sometimes called
 * the WordNet 'supersenses'.
 * 
 * <p>
 * <cite>M. Ciaramita, Y. Altun. 2006 (EMNLP). Broad-Coverage Sense Disambiguation and Information Extraction with a Supersense Sequence Tagger</cite><br/>
 * <cite>M. Ciaramita, M. Johnson. 2003 (ACL). Supersense Tagging of Unknown Nouns in WordNet</cite><br/>
 * <a href="http://sourceforge.net/projects/supersensetag/">SuperSenseTagger</a>
 * 
 * @see http://wordnet.princeton.edu/man/lexnames.5WN.html
 */
// TODO integrate descriptions into API
enum Lexname {
  /** all {@link POS#ADJ adjective} clusters */
  ADJ_ALL("00", "adj.all", ADJ, "adj", "adjective"),
  /** relational {@link POS#ADJ adjective}s ({@link RelationType#PERTAINYM pertainyms}) */
  ADJ_PERT("01", "adj.pert", ADJ, "pert", "pertainym"),
  /** all {@link POS#ADV adverbs} */
  ADV_ALL("02", "adv.all", ADV, "adv", "adverb"),
  /** abstract terms for unique beginners for {@link POS#NOUN nouns} */
  NOUN_TOPS("03", "noun.Tops", NOUN),
  /** {@link POS#NOUN nouns} denoting acts or actions */
  NOUN_ACT("04", "noun.act", NOUN, "act"),
  /** {@link POS#NOUN nouns} denoting animals */
  ANIMALS("05", "noun.animal", NOUN, "animal"),
  /** {@link POS#NOUN nouns} denoting man-made objects */
  NOUN_ARTIFACT("06", "noun.artifact", NOUN, "artifact"),
  /** {@link POS#NOUN nouns} denoting attributes of people and objects */
  NOUN_ATTRIBUTE("07", "noun.attribute", NOUN, "attribute"),
  /** {@link POS#NOUN nouns} denoting body parts */
  NOUN_BODY("08", "noun.body", NOUN),
  /** {@link POS#NOUN nouns} denoting cognitive processes and contents */
  NOUN_COGNITION("09", "noun.cognition", NOUN),
  /** {@link POS#NOUN nouns} denoting communicative processes and contents */
  NOUN_COMMUNICATION("10", "noun.communication", NOUN),
  /** {@link POS#NOUN nouns} denoting natural events */
  NOUN_EVENT("11", "noun.event", NOUN, "event"),
  /** {@link POS#NOUN nouns} denoting feelings and emotions */
  NOUN_FEELING("12", "noun.feeling", NOUN),
  /** {@link POS#NOUN nouns} denoting foods and drinks */
  NOUN_FOOD("13", "noun.food", NOUN, "food"),
  /** {@link POS#NOUN nouns} denoting groupings of people or objects */
  NOUN_GROUP("14", "noun.group", NOUN, "group"),
  /** {@link POS#NOUN nouns} denoting spatial position */
  NOUN_LOCATION("15", "noun.location", NOUN, "location"),
  /** {@link POS#NOUN nouns} denoting goals */
  NOUN_MOTIVE("16", "noun.motive", NOUN, "motive"),
  /** {@link POS#NOUN nouns} denoting natural objects (not man-made) */
  NOUN_OBJECT("17", "noun.object", NOUN, "object"),
  /** {@link POS#NOUN nouns} denoting people */
  NOUN_PERSON("18", "noun.person", NOUN, "person"),
  /** {@link POS#NOUN nouns} denoting natural phenomena */
  NOUN_PHENOMENON("19", "noun.phenomenon", NOUN, "phenomenon"),
  /** {@link POS#NOUN nouns} denoting plants */
  NOUN_PLANT("20", "noun.plant", NOUN, "plant"),
  /** {@link POS#NOUN nouns} denoting possession and transfer of possession */
  NOUN_POSSESSION("21", "noun.possession", NOUN),
  /** {@link POS#NOUN nouns} denoting natural processes */
  NOUN_PROCESS("22", "noun.process", NOUN, "process"),
  /** {@link POS#NOUN nouns} denoting quantities and units of measure */
  NOUN_QUANTITY("23", "noun.quantity", NOUN, "quantity"),
  /** {@link POS#NOUN nouns} denoting relations between people or things or ideas */
  NOUN_RELATION("24", "noun.relation", NOUN, "relation"),
  /** {@link POS#NOUN nouns} denoting two and three dimensional shapes */
  NOUN_SHAPE("25", "noun.shape", NOUN, "shape"),
  /** {@link POS#NOUN nouns} denoting stable states of affairs */
  NOUN_STATE("26", "noun.state", NOUN, "state"),
  /** {@link POS#NOUN nouns} denoting substances */
  NOUN_SUBSTANCE("27", "noun.substance", NOUN, "substance"),
  /** {@link POS#NOUN nouns} denoting time and temporal relations */
  NOUN_TIME("28", "noun.time", NOUN, "time"),
  /** {@link POS#VERB verbs} of grooming, dressing and bodily care */
  VERB_BODY("29", "verb.body", VERB),
  /** {@link POS#VERB verbs} of size, temperature change, intensifying */
  VERB_CHANGE("30", "verb.change", VERB, "change"),
  /** {@link POS#VERB verbs} of thinking, judging, analyzing, doubting */
  VERB_COGNITION("31", "verb.cognition", VERB),
  /** {@link POS#VERB verbs} of telling, asking, ordering, singing */
  VERB_COMMUNICATION("32", "verb.communication", VERB),
  /** {@link POS#VERB verbs} of ﬁghting, athletic activities */
  VERB_COMPETITION("33", "verb.competition", VERB, "competition"),
  /** {@link POS#VERB verbs} of eating and drinking */
  VERB_CONSUMPTION("34", "verb.consumption", VERB, "consumption"),
  /** {@link POS#VERB verbs} of touching, hitting, tying, digging */
  VERB_CONTACT("35", "verb.contact", VERB, "contact"),
  /** {@link POS#VERB verbs} of sewing, baking, painting, performing */
  VERB_CREATION("36", "verb.creation", VERB, "creation"),
  /** {@link POS#VERB verbs} of feeling */
  VERB_EMOTION("37", "verb.emotion", VERB, "emotion"),
  /** {@link POS#VERB verbs} of walking, ﬂying, swimming */
  VERB_MOTION("38", "verb.motion", VERB, "motion"),
  /** {@link POS#VERB verbs} of seeing, hearing, feeling */
  VERB_PERCEPTION("39", "verb.perception", VERB, "perception"),
  /** {@link POS#VERB verbs} of buying, selling, owning */
  VERB_POSSESSION("40", "verb.possession", VERB, "possession"),
  /** {@link POS#VERB verbs} of political and social activities and events */
  VERB_SOCIAL("41", "verb.social", VERB, "social"),
  /** {@link POS#VERB verbs} of being, having, spatial relations */
  VERB_STATIVE("42", "verb.stative", VERB, "stative"),
  /** {@link POS#VERB verbs} of raining, snowing, thawing, thundering */
  VERB_WEATHER("43", "verb.weather", VERB, "weather"),
  /** participial {@link POS#ADJ adjectives} */
  ADJ_PPL("44", "adj.ppl", ADJ, "ppl", "participle", "participial");

  private static final Lexname[] VALUES = values();
  private final String lexNumStr;
  private final String rawLabel;
  private final String[] labels;
  private final POS pos;

  Lexname(final String lexNumStr, final String rawLabel, final POS pos, final String... labels) {
    registerAlias(name(), this);
    registerAlias(name().toLowerCase(), this);
    this.lexNumStr = lexNumStr;
    assert lexNumStr.indexOf(' ') < 0;
    this.rawLabel = rawLabel;
    assert rawLabel.indexOf(' ') < 0;
    registerAlias(rawLabel, this);
    registerAlias(rawLabel.toUpperCase(), this);
    this.labels = labels;
    for (final String label : labels) {
      assert label.indexOf(' ') < 0;
      registerAlias(label, this);
      registerAlias(label.toUpperCase(), this);
    }
    this.pos = pos;
  }

  @Override
  public String toString() {
    return new StringBuilder("[Lexname ").append(rawLabel).append("]").toString();
  }

  POS getPOS() {
    return pos;
  }

  static Lexname lookupLexname(final String label) {
    return ALIASES.get(label);
  }

  static Lexname lookupLexname(final int lexnum) {
    return VALUES[lexnum];
  }

  static String lookupLexCategory(final int lexnum) {
    return lookupLexname(lexnum).rawLabel;
  }
  
  // other (more concise) forms of initialization cause NPE; using lazy init in registerAlias
  // more details http://www.velocityreviews.com/forums/t145807-an-enum-mystery-solved.html
  private static Map<String, Lexname> ALIASES;

  private static void registerAlias(final String form, final Lexname rel) {
    if (ALIASES == null) {
      ALIASES = new HashMap<String, Lexname>();
    }
    final Lexname prev = ALIASES.put(form, rel);
    if (prev != null) {
      // collisions:
      // NOUN_BODY VERB_BODY
      // NOUN_COGNITION VERB_COGNITION
      // NOUN_COMMUNICATION VERB_COMMUNICATION
      // NOUN_POSSESSION VERB_POSSESSION
      System.err.println("  prev: "+prev+" form: "+form+" rel: "+rel);
    }
  }
}