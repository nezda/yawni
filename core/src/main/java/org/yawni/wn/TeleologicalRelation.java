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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The teleological database contains, for approximately 350 artifacts (nouns), an encoding
 * of the typical activity (purpose) for which that artifact was intended. 11 semantic
 * relations are used to encode that activity.
 *
 * @see <a href="http://wordnet.princeton.edu/wordnet/download/standoff/">
 *     http://wordnet.princeton.edu/wordnet/download/standoff/</a>
 */
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
    registerAlias(name(), this);
    registerAlias(name().toLowerCase(), this);
  }

  /** Customized form of {@link #valueOf(java.lang.String)} */
  public static TeleologicalRelation fromValue(final String name) {
    final TeleologicalRelation toReturn = ALIASES.get(name);
    if (toReturn == null) {
      throw new IllegalArgumentException("unknown name");
    }
    return toReturn;
  }

  // other (more concise) forms of initialization cause NPE; using lazy init in registerAlias
  // more details http://www.velocityreviews.com/forums/t145807-an-enum-mystery-solved.html
  private static Map<String, TeleologicalRelation> ALIASES;
  // accessor for testing only
  static Map<String, TeleologicalRelation> getStringToRelMap() {
    return Collections.unmodifiableMap(ALIASES);
  }

  private static void registerAlias(final String form, final TeleologicalRelation rel) {
    if (ALIASES == null) {
      ALIASES = new HashMap<String, TeleologicalRelation>();
    }
    final TeleologicalRelation prev = ALIASES.put(form, rel);
    assert null == prev : "prev: "+prev+" form: "+form+" rel: "+rel;
  }
}
