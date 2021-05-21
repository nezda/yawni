/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wordnet;

import com.google.common.base.Preconditions;
import com.google.common.primitives.SignedBytes;

import java.util.EnumMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yawni.util.CharSequences;
import org.yawni.util.EnumAliases;
import org.yawni.util.LightImmutableList;

/**
 * Handler for {@link WordNet#synsets(String)} and
 * {@link WordNet#wordSenses(String)}.
 */
// Most commands are "filter"s
// Command composition would be sweet
// Builder pattern may simplify this code
enum Command {
  /**
   * if 9 digit, treat 1st digit as POS ordinal; if less digits, require POS;
   * Only compatible with a single, optional POS and other OFFSET(s);
   * if synsets(), return implied Synset, if wordSenses(), return implied WordSense(s)
   */
  OFFSET {
    @Override
    void act(final EnumMap<Command, String> cmdToValue, final WordNet dict) {
      final String value = cmdToValue.get(OFFSET);
      Preconditions.checkArgument(value != null);
      final int num;
      try {
        num = CharSequences.parseInt(value, 10);
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("offset value must be all digits; "+value+" invalid");
      }
      int offset;
      org.yawni.wordnet.POS pos;
      if (value.length() == 9) {
        // special case: 9 digits where leftmost is pos ordinal
        final int posOrd = Character.digit(value.charAt(0), 10);
        pos = org.yawni.wordnet.POS.fromOrdinal(SignedBytes.checkedCast(posOrd));
        if (cmdToValue.containsKey(POS)) {
          // ensure explicitly POS compat with implied POS
          final org.yawni.wordnet.POS explicitPOS = org.yawni.wordnet.POS.valueOf(cmdToValue.get(POS));
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
        pos = org.yawni.wordnet.POS.valueOf(cmdToValue.get(Command.POS));
        offset = num;
      }
      Preconditions.checkArgument(pos != org.yawni.wordnet.POS.ALL, "POS.ALL is not valid with OFFSET");
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
  /** unsupported; filter; alias for WORD */
  SOME_STRING,
  /**
   * unsupported; filter; boolean: indicates WORD is lemma and should not be stemmed
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
    void act(final EnumMap<Command, String> cmdToValue, final WordNet dict) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    String normalizeValue(final String rawValue) {
      if (rawValue.length() == 1) {
        final char c = rawValue.charAt(0);
        if (Character.isDigit(c)) {
          final byte n = Byte.parseByte(rawValue);
          return org.yawni.wordnet.POS.fromOrdinal(n).name();
        } else {
          return org.yawni.wordnet.POS.lookup(c).name();
        }
      } else {
        return org.yawni.wordnet.POS.valueOf(rawValue).name();
      }
    }
  },
  /**
   * unsupported;
   * if synsets(), return implied Synset, if wordSenses(), return implied WordSense
   *
   * not a sensekey, but a simple, often used notation:
   *  {@code <lemma>"#"<pos_letter><senseNumber>}
   *  {@code ambition#n2}
   */
  SENSEKEY,
  /** unsupported; */
  PREFIX,
  /** unsupported; */
  SUBSTRING,
  /** unsupported; key for useful commandline interface */
  RELATION,
  /** unsupported; */
  GLOSS_GREP,
  /**
   * implies POS=ADJ; only applies to POS={ADJ, ALL};
   */
  ADJ_POSITION,
  LEXNAME,
  /** unsupported; */
  RANDOM
  ;

  private static final Logger log = LoggerFactory.getLogger(Command.class);

  private List<String> variants() {
    return LightImmutableList.of(name().toLowerCase(), name());
  }

  /**
   * @param cmdToValue name=value parameter map
   * @param dict
   */
  void act(final EnumMap<Command, String> cmdToValue, final WordNet dict) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  String normalizeValue(final String rawValue) {
    return rawValue;
  }

  Command() {
    for (final String variant : variants()) {
      staticThis.ALIASES.registerAlias(this, variant);
    }
  }

  /** Customized form of {@link #valueOf(String)} */
  static Command fromValue(final String name) {
    return staticThis.ALIASES.valueOf(name);
  }

  private static class staticThis {
    static EnumAliases<Command> ALIASES = EnumAliases.make(Command.class);
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
    final EnumMap<Command, String> cmdToValue = new EnumMap<>(Command.class);
    // parse out <name>"="<value> pairs separated by "&"
    int eidx;
    do {
      eidx = query.indexOf('=', idx);
      if (eidx == -1) {
        throw new IllegalArgumentException("query missing name value delimiter '='");
      }
      final String name = query.substring(idx, eidx);
      idx = eidx + 1;
      eidx = query.indexOf('&', idx);
      if (eidx == -1) {
        eidx = len;
      }
      final String value = query.substring(idx, eidx);
      idx = eidx + 1;
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