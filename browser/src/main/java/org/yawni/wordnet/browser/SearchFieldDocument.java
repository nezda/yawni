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
package org.yawni.wordnet.browser;

import javax.swing.text.*;

/**
 * {@link javax.swing.text.Document} to constrain a search field
 * to non-junk.
 */
class SearchFieldDocument extends PlainDocument {
  private static final int MAX_LEN = 256;

  @Override
  public void insertString(
      final int insOffset,
      String text,
      final AttributeSet attr) throws BadLocationException {
    //// consider whether this insert will match
    //final String proposedInsert =
    //  getText(0, insOffset) +
    //  text +
    //  getText(insOffset, getLength() - insOffset);
    ////System.out.println("proposing to change to: " + proposedInsert);
    //if (matcher != null) {
    //  matcher.reset(proposedInsert);
    //  //System.out.println("matcher reset");
    //  if (false == matcher.matches()) {
    //    //System.out.println("insert doesn't match");
    //    return;
    //  }
    //}

    //System.err.println("input text: "+text);
    // TODO scrub text and then insert the remains
    // shortcut common case: is short and contains nothing but letters or digits
    boolean dirty = isDirty(text, insOffset);
    if (dirty) {
      //System.err.println("dirty insert?: \""+text+"\"");
      text = scrub(text, insOffset);
    }
    super.insertString(insOffset, text, attr);
//    if (dirty) {
//      //System.err.println("dirty doc: "+getText(0, getLength()));
//    }
  }

  @Override
  public void replace(
      final int offset,
      final int length,
      String text,
      final AttributeSet attr) throws BadLocationException {
    if (length != 0 && isDirty(text, offset)) {
      //final String toReplace = getText(offset, length);
      //System.err.println("toReplace: \""+toReplace+"\""+
      //    " (length: "+length+") with \""+text+"\" (length: "+text.length()+")");
      // prevent slash when text is selected from deleting word
      if ("/".equals(text)) {
        return;
      }
      text = scrub(text, offset);
    }
    //System.err.printf("length: %s text: \"%s\"\n", length, text);
    super.replace(offset, length, text, attr);
  }

  // allow all inserts to run, just some wth empty strings
  private String scrub(final String proposedNewText, final int insOffset) {
//    final int currDocLen = getLength();
//    final String prevText;
//    final String nextText;
//    try {
//      prevText = getText(0, insOffset);
//      nextText = currDocLen == insOffset ? "" :
//        getText(insOffset, currDocLen - insOffset);
//    } catch (BadLocationException ble) {
//      throw new RuntimeException(ble);
//    }
//    final int lastChar = prevText.length() != 0 ? prevText.charAt(0) : 0;
    String toInsert = proposedNewText;
    // - bound total document length to MAX_LEN
    toInsert = toInsert.substring(0, Math.min(toInsert.length(), MAX_LEN));
//    // ignore navigation-only slash
//    toInsert = toInsert.replace("/", "");
    // - normalize any internal whitespace
    //   - corner case: allow it to end with up to 1 space or hyphen
    return toInsert;
  }

  private boolean isDirty(final String proposedNewText, final int insOffset) {
    for (int i = 0, len = proposedNewText.length(); i < len; i++) {
      if (! Character.isLetterOrDigit(proposedNewText.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}