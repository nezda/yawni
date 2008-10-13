package edu.brandeis.cs.steele.wn.browser;

import javax.swing.text.*;
import java.util.regex.*;

/**
 * java.swing.text.Document to constrain a search field
 * to non-junk.
 */
class SearchFieldDocument extends PlainDocument {
  private static final long serialVersionUID = 1L;

  private Pattern pattern;
  private Matcher matcher;
  private final int MAX_LEN = 256;

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
    boolean dirty = false == isClean(text, insOffset);
    if(dirty) {
      //System.err.println("dirty insert?: \""+text+"\"");
      text = scrub(text, insOffset);
    }
    super.insertString(insOffset, text, attr);
    if(dirty) {
      //System.err.println("dirty doc: "+getText(0, getLength()));
    }
  }

  @Override
  public void replace(
      final int offset,
      final int length,
      String text,
      final AttributeSet attr) throws BadLocationException {
    if(length != 0 && false == isClean(text, offset)) {
      final String toReplace = getText(offset, length);
      //System.err.println("toReplace: \""+toReplace+"\""+
      //    " (length: "+length+") with \""+text+"\" (length: "+text.length()+")");
      text = scrub(text, offset);
    }
    super.replace(offset, length, text, attr);
  }

  // allow all inserts to run, just some wth empty strings
  private String scrub(final String proposedNewText, final int insOffset) {
    final int currDocLen = getLength();
    final String prevText;
    final String nextText;
    try {
      prevText = getText(0, insOffset);
      nextText = currDocLen == insOffset ? "" :
        getText(insOffset, currDocLen - insOffset);
    } catch(BadLocationException ble) {
      throw new RuntimeException(ble);
    }
    final int lastChar = prevText.length() != 0 ? prevText.charAt(0) : 0;
    String toInsert = proposedNewText;
    // - bound total document length to MAX_LEN
    toInsert = toInsert.substring(0, Math.min(toInsert.length(), MAX_LEN));
    // - normalize any internal whitespace
    //   - corner case: allow it to end with up to 1 space or hyphen

    return toInsert;
  }

  private boolean isClean(final String proposedNewText, final int insOffset) {
    for(int i = 0, len = proposedNewText.length();
        i < len; i++) {
      if(false == Character.isLetterOrDigit(proposedNewText.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
