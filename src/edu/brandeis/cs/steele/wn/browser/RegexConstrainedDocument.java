package edu.brandeis.cs.steele.wn.browser;

import javax.swing.text.*; 
import java.util.regex.*;

/**
 * java.swing.text.Document whose content values are constrained by a regex.
 * Useful to actively control input to a text fields.
 * @author http://www.oreilly.com/catalog/swinghks/
 */
class RegexConstrainedDocument extends PlainDocument {
  private static final long serialVersionUID = 1L;

  private Pattern pattern;
  private Matcher matcher;

  public RegexConstrainedDocument() { 
    super(); 
  }

  public RegexConstrainedDocument(final AbstractDocument.Content c) { 
    super(c); 
  }	

  public RegexConstrainedDocument(final AbstractDocument.Content c, final String p) {
    super(c);
    setPatternByString(p);
  }

  public RegexConstrainedDocument(final String p) {
    super();
    setPatternByString(p);
  }

  void setPatternByString(final String p) {
    final Pattern pattern = Pattern.compile(p);
    // checks the document against the new pattern
    // and removes the content if it no longer matches
    try {
      matcher = pattern.matcher(getText(0, getLength())); 
      //System.out.println("matcher reset to " +	getText(0, getLength()));
      if (false == matcher.matches()) {
        //System.out.println("does not match");
        remove(0, getLength());
      }
    } catch (final BadLocationException ble) {
      ble.printStackTrace(); // impossible?
    }
  }

  @Override
  public void insertString(final int offs, final String s, final AttributeSet a) throws BadLocationException {
    // consider whether this insert will match
    final String proposedInsert =
      getText(0, offs) +
      s +
      getText(offs, getLength() - offs);
    //System.out.println("proposing to change to: " + proposedInsert);
    if (matcher != null) {
      matcher.reset(proposedInsert);
      //System.out.println("matcher reset");
      if (false == matcher.matches()) {
        //System.out.println("insert doesn't match");
        return;
      }
    }
    super.insertString(offs, s, a);
  } 
}
