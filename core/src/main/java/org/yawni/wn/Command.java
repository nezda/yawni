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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yawni.util.CharSequences;
import org.yawni.util.ImmutableList;
import org.yawni.util.Preconditions;

/**
 * Handler for {@link FileBackedDictionary#synsets(java.lang.String)} and
 * {@link FileBackedDictionary#wordSenses(java.lang.String)}.
 */
enum Command {
  /**
   * if 9 digit, treat 1st digit as POS ordinal; if less digits, require POS;
   * Only compatible with a single, optional POS and other OFFSET(s);
   * if synsets(), return implied Synset, if wordSenses(), return implied WordSense(s)
   */
  OFFSET {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
      final String value = cmdToValue.get(OFFSET);
      Preconditions.checkArgument(value != null);
      final int num;
      try {
        num = CharSequences.parseInt(value, 10);
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("offset value must be all digits; "+value+" invalid");
      }
      int offset;
      org.yawni.wn.POS pos;
      if (value.length() == 9) {
        // special case: 9 digits where leftmost is pos ordinal
        final int posOrd = Character.digit(value.charAt(0), 10);
        pos = org.yawni.wn.POS.fromOrdinal((byte)posOrd);
        if (cmdToValue.containsKey(POS)) {
          // ensure explicity POS compat with implied POS
          final org.yawni.wn.POS explicitPOS = org.yawni.wn.POS.valueOf(cmdToValue.get(POS));
          Preconditions.checkArgument(pos == explicitPOS,
            "inconsistent POS; explicit POS: "+explicitPOS+" implied POS: "+pos);
        }
        cmdToValue.put(POS, pos.name());
        offset = CharSequences.parseInt(value, 1, 9);
        // replace OFFSET's value in cmdToValue
        cmdToValue.put(OFFSET, value.substring(1, 9));
      } else {
        Preconditions.checkArgument(cmdToValue.containsKey(Command.POS), "POS required for plain OFFSET query");
        // note POS.valueOf will throw NPE if value is null
        pos = org.yawni.wn.POS.valueOf(cmdToValue.get(Command.POS));
        offset = num;
      }
      Preconditions.checkArgument(pos != org.yawni.wn.POS.ALL, "POS.ALL is not valid with OFFSET");
//      final Synset syn = dict.getSynsetAt(pos, offset);
//      throw new UnsupportedOperationException("Not yet implemented");
    }
  },
  /**
   * "SOMESTRING" might be a better name;
   * some string to match fully (including after stemming)
   * consult LEMMA;
   * if synsets(), return implied Synset (i.e., lookupSynsets()), if wordSenses(), return implied WordSense(s) (i.e., lookupWordSenses())
   */
  WORD,
  /** filter; alias for WORD */
  SOME_STRING,
  /**
   * filter; boolean: indicates WORD is lemma and should not be stemmed
   * default true;
   */
  LEMMA,
  /**
   * filter; default ALL;
   * support any of {NOUN, Noun, N, n, 1};
   * if only POS is provided, return synsets(POS)
   * if synsets(), return implied Synset (i.e., lookupSynsets()), if wordSenses(), return implied WordSense(s) (i.e., lookupWordSenses())
   */
  POS {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    String normalizeValue(final String rawValue) {
      if (rawValue.length() == 1) {
        final char c = rawValue.charAt(0);
        if (Character.isDigit(c)) {
          final byte n = Byte.parseByte(rawValue);
          return org.yawni.wn.POS.fromOrdinal(n).name();
        } else {
          return org.yawni.wn.POS.lookup(c).name();
        }
      } else {
        return org.yawni.wn.POS.valueOf(rawValue).name();
      }
    }
  },
  /**
   * if synsets(), return implied Synset, if wordSenses(), return implied WordSense
   */
  SENSEKEY {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  },
  PREFIX {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  },
  SUBSTRING {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  },
  GLOSS_GREP {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  },
  /**
   * implies POS=ADJ; only applies to POS={ADJ, ALL};
   */
  ADJ_POSITION,
  LEXNAME,
  RANDOM
  ;

  private static final Logger log = LoggerFactory.getLogger(Command.class.getName());

  private List<String> variants() {
    return ImmutableList.of(name().toLowerCase(), name());
  }

  /**
   * @param cmdToValue name=value parameter map
   * @param dict
   */
  void act(final EnumMap<Command, String> cmdToValue, final FileBackedDictionary dict) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  String normalizeValue(final String rawValue) {
    return rawValue;
  }

  private Command() {
    for (final String variant : variants()) {
      registerAlias(variant, this);
    }
  }

  /** Customized form of {@link #valueOf(java.lang.String)} */
  static Command fromValue(final String name) {
    final Command toReturn = ALIASES.get(name);
    Preconditions.checkArgument(toReturn != null, "unknown command name: "+name);
    return toReturn;
  }

  // other (more concise) forms of initialization cause NPE; using lazy init in registerAlias
  // more details http://www.velocityreviews.com/forums/t145807-an-enum-mystery-solved.html
  private static Map<String, Command> ALIASES;
  private static void registerAlias(final String form, final Command rel) {
    if (ALIASES == null) {
      ALIASES = new java.util.HashMap<String, Command>();
    }
    final Command prev = ALIASES.put(form, rel);
    assert null == prev : "prev: "+prev+" form: "+form+" rel: "+rel;
  }

  /**
   * parse out query using standard URI "query" syntax:<pre>{@code
   *   "?"<command>"="<value>("&"<command>"="<value>)*
   * }</pre>
   * issues:
   * what if query includes URI escaped text? (e.g., "%20" == " ")
   */
  static EnumMap<Command, String> getCmdToValue(final String query) {
    Preconditions.checkNotNull(query);
    if (! query.startsWith("?")) {
      throw new IllegalArgumentException("missing leading query indicating '?'");
    }
    int idx = 1;
    final int len = query.length();
    final EnumMap<Command, String> cmdToValue = new EnumMap<Command, String>(Command.class);
    // parse out <name>"="<value> pairs separated by "&"
    int eidx;
    do {
      eidx = query.indexOf("=", idx);
      if (eidx == -1) {
        throw new IllegalArgumentException("query missing name value delimiter '='");
      }
      final String name = query.substring(idx, eidx);
      idx = eidx + 1;
      eidx = query.indexOf("&", idx);
      if (eidx == -1) {
        eidx = len;
      }
      final String value = query.substring(idx, eidx);
      idx = eidx == -1 ? len : eidx + 1;
      log.trace("name: {} value: {}", name, value);
      final Command cmd = Command.fromValue(name);
      // NOTE: normalizeValue throws
      final String prev = cmdToValue.put(cmd, cmd.normalizeValue(value));
      Preconditions.checkArgument(prev == null,
        "command repetition not supported; Existing value "+prev+" found for "+cmd+" value "+value);
    } while (eidx != len);

    log.trace("cmdToValue: {}", cmdToValue);
    return cmdToValue;
  }
}