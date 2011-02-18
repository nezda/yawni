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
package org.yawni.wordnet;

import com.google.common.annotations.VisibleForTesting;
import org.yawni.util.EnumAliases;

/**
 * Morpho-semantic relations between morphologically related nouns and verbs, parallel
 * with {@link RelationType#DERIVATIONALLY_RELATED} {@link Relation}s.  This work is outlined
 * in the 2007 paper "Putting Semantics into WordNet's "Morphosemantic" Links" by Fellbaum, et al.
 *
 * @see <a href="http://wordnet.princeton.edu/wordnet/download/standoff/">
 *     http://wordnet.princeton.edu/wordnet/download/standoff/</a>
 */
enum MorphosemanticRelation {
  /** 
   * e.g., employ/employment (in essence an identity or equality relation)
   */
  EVENT("Event", "Event"), // 8158 instances
  /**
   * e.g., employ/employer, invent/inventor, produce/producer;
   * (nouns denote the Agents of the events referred to by the verbs);
   * 'employer' is the Agent of the event denoted by 'employ',
   * 'inventor' is the Agent of the event denoted by 'invent',
   * 'producer' is the Agent of the event denoted by 'produce'
   */
  AGENT("Agent", "Agent"),  // 3043 instances
  /**
   * aka Cause
   * e.g., liquify/liquid
   */
  RESULT("Result", "Result"), // 1439 instances
  /** 
   * e.g., dilate/dilator; sense-sensor
   */
  BY_MEANS_OF("by-means-of", "By-means-of", "By-means-of"), // 1273 instances
  /**
   * aka Patient
   * e.g., employ/employee
   */
  UNDERGOER("Undergoer", "Undergoer"), // 878 instances
  /**
   * An Instrument does not act alone but implies an Agent who controls it, usually with intention;
   * e.g., poke/poker, shred/shredder
   */
  INSTRUMENT("Instrument", "Instrument"), // 813 instances
  /**
   * aka Purpose/Function
   * e.g., harness/harness
   */
  USES("Uses", "Uses"), // 740 instances
  /** e.g., transcend/transcendence */
  STATE("State", "State"), // 528 instances
  /** e.g., cool/cool */
  PROPERTY("Property", "Property"), // 318 instances
  /** e.g., bath/bath */
  LOCATION("Location", "Location"), // 288 instances
  /**
   * aka Inanimate Agent/Cause
   * e.g., insulate/insulator
   */
  MATERIAL("Location", "Location"), // 114 intances
  /** e.g., kayak/kayak; cruise/cruiser */
  VEHICLE("Vehicle", "Vehicle"), // 87 instances
  /** e.g., abduct/abductor */
  BODY_PART("body-part", "Body part", "Body part"), // 43 instances
  /** e.g., tee/tee */
  DESTINATION("Destination", "Destination"), // 17 instances
  ;

  final String longNounLabel;
  final String longVerbLabel;
  
  private MorphosemanticRelation(final String shallowForm, final String longNounLabel, final String longVerbLabel) {
    this(longNounLabel, longVerbLabel);
    staticThis.ALIASES.registerAlias(this, shallowForm.toLowerCase(), shallowForm.toUpperCase());
  }
  
  private MorphosemanticRelation(final String longNounLabel, final String longVerbLabel) {
    staticThis.ALIASES.registerAlias(this, name(), name().toLowerCase());
    this.longNounLabel = longNounLabel;
    this.longVerbLabel = longVerbLabel;
  }

  /** Customized form of {@link #valueOf(String)} */
  public static MorphosemanticRelation fromValue(final String name) {
    return staticThis.ALIASES.valueOf(name);
  }

  @VisibleForTesting
  static String aliases() {
    return staticThis.ALIASES.toString();
  }
  
  private static class staticThis {
    static EnumAliases<MorphosemanticRelation> ALIASES = EnumAliases.make(MorphosemanticRelation.class);
  }
}