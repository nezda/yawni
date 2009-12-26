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

/**
 * Represents the contents of (WordNet 3.0) lexnames since it is
 * "optional".  These are the WordNet lexicographers classes, sometimes called
 * the WordNet 'supersenses'.
 * <cite>M. Ciaramita, Y. Altun. 2006. Broad-Coverage Sense Disambiguation and Information Extraction with a Supersense Sequence Tagger</cite>
 * Supersense Tagging of Unknown Nouns in WordNet
 * TODO integrate descriptions into API
 * @see http://wordnet.princeton.edu/man/lexnames.5WN.html
 */
enum Lexname {
  /** all {@link POS#ADJ adjective} clusters */
  ADJ_ALL("00", "adj.all", "3"),
  /** relational {@link POS#ADJ adjective}s ({@link RelationType#PERTAINYM pertainyms}) */
  ADJ_PERT("01", "adj.pert", "3"),
  /** all {@link POS#ADV adverbs} */
  ADV_ALL("02", "adv.all", "4"),
  /** abstract terms for unique beginners for {@link POS#NOUN nouns} */
  NOUN_TOPS("03", "noun.Tops", "1"),
  /** {@link POS#NOUN nouns} denoting acts or actions */
  NOUN_ACT("04", "noun.act", "1"),
  /** {@link POS#NOUN nouns} denoting animals */
  ANIMALS("05", "noun.animal", "1"),
  /** {@link POS#NOUN nouns} denoting man-made objects */
  NOUN_ARTIFACT("06", "noun.artifact", "1"),
  /** {@link POS#NOUN nouns} denoting attributes of people and objects */
  NOUN_ATTRIBUTE("07", "noun.attribute", "1"),
  /** {@link POS#NOUN nouns} denoting body parts */
  NOUN_BODY("08", "noun.body", "1"),
  /** {@link POS#NOUN nouns} denoting cognitive processes and contents */
  NOUN_COGNITION("09", "noun.cognition", "1"),
  /** {@link POS#NOUN nouns} denoting communicative processes and contents */
  NOUN_COMMUNICATION("10", "noun.communication", "1"), 
  /** {@link POS#NOUN nouns} denoting natural events */
  NOUN_EVENT("11", "noun.event", "1"), 
  /** {@link POS#NOUN nouns} denoting feelings and emotions */
  NOUN_FEELING("12", "noun.feeling", "1"), 
  /** {@link POS#NOUN nouns} denoting foods and drinks */
  NOUN_FOOD("13", "noun.food", "1"), 
  /** {@link POS#NOUN nouns} denoting groupings of people or objects */
  NOUN_GROUP("14", "noun.group", "1"), 
  /** {@link POS#NOUN nouns} denoting spatial position */
  NOUN_LOCATION("15", "noun.location", "1"), 
  /** {@link POS#NOUN nouns} denoting goals */
  NOUN_MOTIVE("16", "noun.motive", "1"), 
  /** {@link POS#NOUN nouns} denoting natural objects (not man-made) */
  NOUN_OBJECT("17", "noun.object", "1"), 
  /** {@link POS#NOUN nouns} denoting people */
  NOUN_PERSON("18", "noun.person", "1"), 
  /** {@link POS#NOUN nouns} denoting natural phenomena */
  NOUN_PHENOMENON("19", "noun.phenomenon", "1"), 
  /** {@link POS#NOUN nouns} denoting plants */
  NOUN_PLANT("20", "noun.plant", "1"), 
  /** {@link POS#NOUN nouns} denoting possession and transfer of possession */
  NOUN_POSSESSION("21", "noun.possession", "1"), 
  /** {@link POS#NOUN nouns} denoting natural processes */
  NOUN_PROCESS("22", "noun.process", "1"), 
  /** {@link POS#NOUN nouns} denoting quantities and units of measure */
  NOUN_QUANTITY("23", "noun.quantity", "1"), 
  /** {@link POS#NOUN nouns} denoting relations between people or things or ideas */
  NOUN_RELATION("24", "noun.relation", "1"), 
  /** {@link POS#NOUN nouns} denoting two and three dimensional shapes */
  NOUN_SHAPE("25", "noun.shape", "1"), 
  /** {@link POS#NOUN nouns} denoting stable states of affairs */
  NOUN_STATE("26", "noun.state", "1"), 
  /** {@link POS#NOUN nouns} denoting substances */
  NOUN_SUBSTANCE("27", "noun.substance", "1"), 
  /** {@link POS#NOUN nouns} denoting time and temporal relations */
  NOUN_TIME("28", "noun.time", "1"), 
  /** {@link POS#VERB verbs} of grooming, dressing and bodily care */
  VERB_BODY("29", "verb.body", "2"), 
  /** {@link POS#VERB verbs} of size, temperature change, intensifying */
  VERB_CHANGE("30", "verb.change", "2"), 
  /** {@link POS#VERB verbs} of thinking, judging, analyzing, doubting */
  VERB_COGNITION("31", "verb.cognition", "2"), 
  /** {@link POS#VERB verbs} of telling, asking, ordering, singing */
  VERB_COMMUNICATION("32", "verb.communication", "2"), 
  /** {@link POS#VERB verbs} of ﬁghting, athletic activities */
  VERB_COMPETITION("33", "verb.competition", "2"), 
  /** {@link POS#VERB verbs} of eating and drinking */
  VERB_CONSUMPTION("34", "verb.consumption", "2"), 
  /** {@link POS#VERB verbs} of touching, hitting, tying, digging */
  VERB_CONTACT("35", "verb.contact", "2"), 
  /** {@link POS#VERB verbs} of sewing, baking, painting, performing */
  VERB_CREATION("36", "verb.creation", "2"), 
  /** {@link POS#VERB verbs} of feeling */
  VERB_EMOTION("37", "verb.emotion", "2"), 
  /** {@link POS#VERB verbs} of walking, ﬂying, swimming */
  VERB_MOTION("38", "verb.motion", "2"), 
  /** {@link POS#VERB verbs} of seeing, hearing, feeling */
  VERB_PERCEPTION("39", "verb.perception", "2"), 
  /** {@link POS#VERB verbs} of buying, selling, owning */
  VERB_POSSESSION("40", "verb.possession", "2"), 
  /** {@link POS#VERB verbs} of political and social activities and events */
  VERB_SOCIAL("41", "verb.social", "2"), 
  /** {@link POS#VERB verbs} of being, having, spatial relations */
  VERB_STATIVE("42", "verb.stative", "2"), 
  /** {@link POS#VERB verbs} of raining, snowing, thawing, thundering */
  VERB_WEATHER("43", "verb.weather", "2"), 
  /** participial {@link POS#ADJ adjectives} */
  ADJ_PPL("44", "adj.ppl", "3");

  private static final Lexname[] VALUES = values();
  private final String lexNumStr;
  private final String label;
  private final String posOrdinalStr;

  Lexname(final String lexNumStr, final String label, final String posOrdinalStr) {
    this.lexNumStr = lexNumStr;
    assert lexNumStr.indexOf(' ') < 0;
    this.label = label;
    assert label.indexOf(' ') < 0;
    this.posOrdinalStr = posOrdinalStr;
    assert posOrdinalStr.indexOf(' ') < 0;
  }

  static String lookupLexCategory(final int lexnum) {
    return VALUES[lexnum].label;
  }
}