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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Morpho-semantic relations between morphologically related nouns and verbs, parallel
 * with {@link RelationType#DERIVATIONALLY_RELATED} {@link Relation}s.  This work is outlined
 * in the 2007 paper "Putting Semantics into WordNet's "Morphosemantic" Links" by Fellbaum, et al.
 *
 * @see <a href="http://wordnet.princeton.edu/wordnet/download/standoff/">
 *     http://wordnet.princeton.edu/wordnet/download/standoff/</a>
 */
enum MorphoSemanticRelation {
  /** e.g., employ/employment (in essence an identity or equality relation) */
  EVENT, // 8158 instances
  /** e.g., employer/employ, inventor/invent, producer/produce */
  AGENT,  // 3043 instances
  /**
   * aka Cause
   * e.g., liquify/liquid
   */
  RESULT, // 1439 instances
  /** e.g., dilate/dilator; sense-sensor */
  BY_MEANS_OF("by-means-of"), // 1273 instances
  /**
   * aka Patient
   * e.g., employee/employ
   */
  UNDERGOER, // 878 instances
  /**
   * An Instrument does not act alone but implies an Agent who controls it, usually with intention;
   * e.g., poke/poker
   */
  INSTRUMENT, // 813 instances
  /**
   * aka Purpose/Function
   * e.g., harness/harness; train/trainer (shoes)
   */
  USES, // 740 instances
  /** e.g., transcend/transcendence */
  STATE, // 528 instances
  /** e.g., cool/cool */
  PROPERTY, // 318 instances
  /** e.g., bath/bath */
  LOCATION, // 288 instances
  /**
   * aka Inanimate Agent/Cause
   * e.g., insulate/insulator
   */
  MATERIAL, // 114 intances
  /** e.g., kayak/kayak; cruiser/cruise */
  VEHICLE, // 87 instances
  /** e.g., abduct/abductor */
  BODY_PART("body-part"), // 43 instances
  /** e.g., tee/tee */
  DESTINATION, // 17 instances
  ;
  
  private MorphoSemanticRelation(final String shallowForm) {
    this();
    registerAlias(shallowForm.toLowerCase(), this);
    registerAlias(shallowForm.toUpperCase(), this);
  }
  
  private MorphoSemanticRelation() {
    registerAlias(name(), this);
    registerAlias(name().toLowerCase(), this);
  }

  /** Customized form of {@link #valueOf(String)} */
  public static MorphoSemanticRelation fromValue(final String name) {
    final MorphoSemanticRelation toReturn = ALIASES.get(name);
    if (toReturn == null) {
      throw new IllegalArgumentException("unknown name");
    }
    return toReturn;
  }

  // other (more concise) forms of initialization cause NPE; using lazy init in registerAlias
  // more details http://www.velocityreviews.com/forums/t145807-an-enum-mystery-solved.html
  private static Map<String, MorphoSemanticRelation> ALIASES;
  // accessor for testing only
  static Map<String, MorphoSemanticRelation> getStringToRelMap() {
    return Collections.unmodifiableMap(ALIASES);
  }
  
  private static void registerAlias(final String form, final MorphoSemanticRelation rel) {
    if (ALIASES == null) {
      ALIASES = new HashMap<String, MorphoSemanticRelation>();
    }
    ALIASES.put(form, rel);
  }
}