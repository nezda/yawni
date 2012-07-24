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
 * The teleological database contains, for approximately 350 artifacts (nouns), an encoding
 * of the typical activity (purpose) for which that artifact was intended. 11 semantic
 * relations are used to encode that activity.
 *
 * @see <a href="http://wordnet.princeton.edu/wordnet/download/standoff/">
 *     http://wordnet.princeton.edu/wordnet/download/standoff/</a>
 */
// TODO integrate into API:
// * add RelationType.RelationTypeType.TELEOLOGICAL
// * needs to be factored into every search and spliced in with "normal" relations like MORPHOSEMATIC is (these
//   were easy to piggyback on with existing DERIVATIONALLY_RELATED (Synset#addExtraMorphosemanticRelations))
// * consider just building the 1,052 instances at load time
enum TeleologicalRelation {
  ACTION, // 448 instances
  THEME, // 214 instances
  RESULT, // 109 instances
  AGENT, // 77 instances
  LOCATION, // 57 instances
  UNDERGOER, // 47 instances
  INSTRUMENT, // 25 instances
  BENEFICIARY, // 23 instances
  DESTINATION, // 18 instances
  CAUSE, // 17 instances
  EXPERIENCER, // 10 instances
  SOURCE, // 8 instances
  ;

  private TeleologicalRelation() {
    staticThis.ALIASES.registerAlias(this, name(), name().toLowerCase());
  }

  /** Customized form of {@link #valueOf(String)} */
  public static TeleologicalRelation fromValue(final String name) {
    return staticThis.ALIASES.valueOf(name);
  }

  @VisibleForTesting
  static String aliases() {
    return staticThis.ALIASES.toString();
  }

  private static class staticThis {
    static EnumAliases<TeleologicalRelation> ALIASES = EnumAliases.make(TeleologicalRelation.class);
  }
}