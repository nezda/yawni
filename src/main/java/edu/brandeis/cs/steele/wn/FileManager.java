/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.*;
import java.util.logging.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;
import java.io.*;
import edu.brandeis.cs.steele.util.Utils;

/** An implementation of <code>FileManagerInterface</code> that reads files
 * from the local file system.  A file  <code>FileManager</code> caches the
 * file position before and after {@link FileManagerInterface#readLineAt
 * FileManagerInterface.readLineAt()} in order to eliminate the redundant IO
 * activity that a naive implementation of these methods would necessitate.
 *
 * <p>Instances of this class are guarded.  All operations are read-only, but
 * are synchronized per file to maintain state including the file pointer's
 * position.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class FileManager implements FileManagerInterface {
  //
  // Class variables
  //
  //intentionally using FileBackedDictionary's logger (for now)
  private static final Logger log = Logger.getLogger("edu.brandeis.cs.steele.wn.FileBackedDictionary");

  /** The API version, used by <code>RemoteFileManager</code> for constructing a binding name. */
  public static final String VERSION = "2.0.0";

  //
  // Instance variables
  //
  private String searchDirectory;
  private Map<String, CharStream> filenameCache = new HashMap<String, CharStream>();

  static class NextLineCache {
    private String filename;
    private int previous;
    private int next;

    synchronized void setNextLineOffset(final String filename, final int previous, final int next) {
      this.filename = filename;
      this.previous = previous;
      this.next = next;
    }

    synchronized int matchingOffset(final String filename, final int offset) {
      if (this.filename == null ||
          previous != offset ||
          //false == this.filename.equals(filename)
          false == this.filename.equals(filename)
          ) {
        return -1;
      } else {
        return next;
      }
    }
  } // end class NextLineCache
  private NextLineCache nextLineCache = new NextLineCache();

  //
  // Constructors
  //
  /** FIXME
   * Construct a file manager backed by a set of files contained in the default WordNet search directory.
   * The default search directory is the location named by the system property WNSEARCHDIR; or, if this
   * is undefined, by the directory named WNHOME/Database (under MacOS) or WNHOME/dict (otherwise);
   * or, if the WNHOME is undefined, the current directory (under MacOS), "C:\wn16" (Windows),
   * or "/usr/local/wordnet1.6" (otherwise).
   */
  public FileManager() {
    this(getWNSearchDir());
  }

  /** Construct a file manager backed by a set of files contained in <var>searchDirectory</var>. */
  public FileManager(String searchDirectory) {
    this.searchDirectory = searchDirectory;
  }

  static String getWNHome() {
    //FIXME see notes in getWNSearchDir()
    String home = System.getProperty("WNHOME");
    if (home != null && new File(home).exists()) {
      return home;
    } else {
      home = System.getenv("WNHOME");
      if (home != null && new File(home).exists()) {
        return home;
      }
    }
    log.log(Level.SEVERE, "WNHOME is not defined correctly as either a Java system property or environment variable. "+
        System.getenv()+" \n\nsystem properties: "+System.getProperties());
    throw new IllegalStateException("WNHOME is not defined correctly as either a Java system property or environment variable. "+
        System.getenv()+" \n\nsystem properties: "+System.getProperties());
  }

  static String getWNSearchDir() {
    //FIXME unify logic for this (getWNSearchDir()) and getWNHome() to try both
    //system property AND environment variables and check readable
    String searchDir = System.getProperty("WNSEARCHDIR");
    if (searchDir != null && new File(searchDir).exists()) {
      return searchDir;
    }
    searchDir = System.getenv("WNSEARCHDIR");
    if (searchDir != null && new File(searchDir).exists()) {
      return searchDir;
    }
    return getWNHome() + File.separator + "dict";
  }

  //
  // IO primitives
  //

  // NOTE: CharStream is not thread-safe
  static abstract class CharStream {
    abstract void seek(final int position) throws IOException;
    abstract int position() throws IOException;
    abstract char charAt(int position) throws IOException;
    abstract int length() throws IOException;
    /** This works just like {@link RandomAccessFile#readLine} -- doesn't
     * support Unicode
     */
    abstract String readLine() throws IOException;
    void skipLine() throws IOException {
      readLine();
    }
    String readLineWord() throws IOException {
      final String ret = readLine();
      if (ret == null) {
        return null;
      }
      final int space = ret.indexOf(' ');
      assert space >= 0;
      return ret.substring(0, space);
    }
    /**
     * Treat file contents like an array of lines and return the zero-based,
     * inclusive line corresponding to <var>linenum</var>
     */
    String readLineNumber(int linenum) throws IOException {
      //TODO when creating the CharStream, add option to "index"/cache these results as either String[] OR String[][]
      //where each row is an array of the delimted items on it and a second optional argument
      //readLineNumber(int linenum, int wordnum)
      //assumption is these CharStream's will be tiny
      //and we can still lazy load this
      seek(0);
      for (int i = 0; i < linenum; i++) {
        skipLine();
      }
      return readLine();
    }
  } // end class CharStream

  static class RAFCharStream extends CharStream {
    private final RandomAccessFile raf;
    RAFCharStream(final RandomAccessFile raf) {
      this.raf = raf;
    }
    @Override void seek(final int position) throws IOException {
      raf.seek(position);
    }
    @Override int position() throws IOException {
      return (int) raf.getFilePointer();
    }
    @Override char charAt(int position) throws IOException {
      seek(position);
      return (char)raf.readByte();
    }
    @Override int length() throws IOException {
      return (int) raf.length();
    }
    @Override String readLine() throws IOException {
      return raf.readLine();
    }
  } // end class RAFCharStream

  private static class NIOCharStream extends CharStream {
    //FIXME position seems redundant (ByteCharBuffer has position())
    protected int position;
    //protected final ByteBuffer buf;
    protected final ByteCharBuffer buf;

    NIOCharStream(final RandomAccessFile raf) throws IOException {
      final FileChannel fileChannel = raf.getChannel();
      final long size = fileChannel.size();
      // program logic currently depends on the entire file being mapped into memory
      // size /= 2;
      final MappedByteBuffer mmap = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
      // this buffer isDirect()
      //log.log(Level.FINE, "mmap.fine(): {0}", mmap.isDirect());
      //this.buf = mmap;
      this.buf = new ByteCharBuffer(mmap, false);
    }
    @Override void seek(final int position) throws IOException {
      // buffer cannot exceed Integer.MAX_VALUE since arrays are limited by this
      this.position = position;
    }
    @Override int position() throws IOException {
      return position;
    }
    @Override char charAt(final int p) throws IOException {
      return (char) buf.get(p);
    }
    @Override int length() throws IOException {
      return buf.capacity();
    }
    @Override String readLine() throws IOException {
      final int s = position;
      final int e = scanForwardToLineBreak();
      assert s >= 0;
      assert e >= 0;
      final int len = e - s;
      if (len <= 0) {
        return null;
      }
      final char[] line = new char[len];
      for (int j = s, i = 0; i < line.length; i++, j++) {
        // NOTE: casting byte to char here since WordNet is currently
        // only ASCII
        line[i] = (char) buf.get(j);
      }
      final String toReturn = new String(line);
      return toReturn;
    }
    @Override void skipLine() throws IOException {
      scanForwardToLineBreak();
    }
    @Override String readLineWord() throws IOException {
      final int s = position;
      int e = scanForwardToLineBreak();
      assert s >= 0;
      assert e >= 0;
      int len = e - s;
      if (len <= 0) {
        return null;
      }
      e = scanToSpace(s);
      len = e - s;
      final char[] word = new char[len];
      for (int j = s, i = 0; i < word.length; i++, j++) {
        // NOTE: casting byte to char here since WordNet is currently
        // only ASCII
        word[i] = (char) buf.get(j);
      }
      final String toReturn = new String(word);
      return toReturn;
    }
    protected int scanToSpace(int s) {
      // scan from current position to first ' '
      char c;
      while (s < buf.capacity()) {
        c = (char) buf.get(s++);
        if (c == ' ') {
          return s - 1;
        }
      }
      throw new IllegalStateException();
    }
    protected int scanForwardToLineBreak() {
      // scan from current position to first ("\r\n"|"\r"|"\n")
      boolean done = false;
      boolean crnl = false;
      char c;
      while (done == false && position < buf.capacity()) {
        c = (char) buf.get(position++);
        switch(c) {
          case '\r':
            // if next is \n, skip that too
            c = (char) buf.get(position++);
            if (c != '\n') {
              // put it back
              --position;
            } else {
              crnl = true;
            }
            done = true;
            break;
          case '\n':
            done = true;
            break;
          default:
            // no-op
        }
      }
      // return exclusive end chopping line break delimitter(s)
      return crnl ? position - 2 : position - 1;
    }
    protected int scanBackwardToLineBreak() {
      // scan backwards to first \n
      // - if immediately preceding char is \n, keep going
      throw new UnsupportedOperationException();
    }
  } // end class NIOCharStream

  /**
   * Like a read-only {@link CharBuffer} made from a {@link ByteBuffer} with a
   * stride of 1 instead of 2.
   */
  private static class ByteCharBuffer implements CharSequence {
    private final ByteBuffer bb;
    ByteCharBuffer(final ByteBuffer bb) {
      this(bb, true);
    }
    ByteCharBuffer(final ByteBuffer bb, final boolean dupAndClear) {
      if (dupAndClear) {
        this.bb = bb.duplicate();
        this.bb.clear();
      } else {
        this.bb = bb;
      }
    }
    public int capacity() { return bb.capacity(); }
    public ByteCharBuffer clear() { bb.clear(); return this; }
    public ByteCharBuffer duplicate() {
      return new ByteCharBuffer(bb.duplicate(), false);
    }
    public ByteCharBuffer flip() { bb.flip(); return this; }
    public char get() { return (char) bb.get(); }
    public char get(final int index) { return (char) bb.get(index); }
    public boolean hasRemaining() { return bb.hasRemaining(); }
    public boolean isDirect() { return bb.isDirect(); }
    public ByteCharBuffer slice() {
      return new ByteCharBuffer(bb.slice(), false);
    }
    public int limit() { return bb.limit(); }
    public ByteCharBuffer limit(final int newLimit){ bb.limit(newLimit); return this; }
    public ByteCharBuffer mark() { bb.mark(); return this; }
    public int position() { return bb.position(); }
    public ByteCharBuffer position(final int newPosition) { bb.position(newPosition); return this; }
    public int remaining() { return bb.remaining(); }
    public ByteCharBuffer reset() { bb.reset(); return this; }
    public ByteCharBuffer rewind() { bb.rewind(); return this; }
    /** @inheritDoc */
    public char charAt(final int index) { return get(index); }
    /** @inheritDoc */
    public int length() { return bb.remaining(); }
    /** @inheritDoc */
    public CharSequence subSequence(final int start, final int end) {
      // XXX not sure if a slice should be used here
      throw new UnsupportedOperationException("TODO IMPLEMENT ME");
      // start and end are relative to position
      // this operation should not change position though
      // so cannot simply "return this;"
      // (position()+start, position()+end]
    }
    @Override public String toString() {
      throw new UnsupportedOperationException("TODO IMPLEMENT ME");
    }
  } // end class ByteCharBuffer

  synchronized CharStream getFileStream(final String filename) throws IOException {
    return getFileStream(filename, true);
  }

  /**
   * @param filename
   * @param filenameWnRelative is a boolean which indicates that <param>filename</param>
   * is relative (or absolute).  This facilitates testing and reuse.
   * @return CharStream representing <param>filename</param> or null if no such file exists.
   */
  synchronized CharStream getFileStream(final String filename, final boolean filenameWnRelative) throws IOException {
    CharStream stream = filenameCache.get(filename);
    if (stream == null) {
      final String pathname =
        filenameWnRelative ? searchDirectory + File.separator + filename :
        filename;
      final File file = new File(pathname);
      if (file.exists() && file.canRead()) {
        //slow CharStream
        //stream = new RAFCharStream(new RandomAccessFile(pathname, "r"));
        //fast CharStream stream
        stream = new NIOCharStream(new RandomAccessFile(file, "r"));
      } else {
        //TODO throw an exception to indicate that pathname is non existant/readble
      }
      filenameCache.put(filename, stream);
    }
    return stream;
  }

  //
  // Line-based interface methods
  //

  /**
   * {@inheritDoc}
   */
  public String readLineNumber(final int linenum, final String filename) throws IOException {
    final CharStream stream = getFileStream(filename);
    if (stream == null) {
      return null;
    }
    synchronized (stream) {
      return stream.readLineNumber(linenum);
    }
  }

  /**
   * {@inheritDoc}
   */
  // Core search routine.  Only called from within synchronized blocks.
  public String readLineAt(final int offset, final String filename) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      final String line = stream.readLine();

      int nextOffset = stream.position();
      if (line == null) {
        nextOffset = -1;
      }
      nextLineCache.setNextLineOffset(filename, offset, nextOffset);
      return line;
    }
  }

  /**
   * {@inheritDoc}
   */
  // Core search routine.  Only called from within synchronized blocks.
  public int getNextLinePointer(final int offset, final String filename) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      final int next;
      if (0 <= (next = nextLineCache.matchingOffset(filename, offset))) {
        return next;
      }
      stream.seek(offset);
      stream.skipLine();
      return stream.position();
    }
  }

  //
  // Low-level Searching
  //

  /**
   * {@inheritDoc}
   */
  // used by substring search iterator
  public int getMatchingLinePointer(int offset, final CharSequence substring, final String filename) throws IOException {
    if (substring.length() == 0) {
      return -1;
    }
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String line = stream.readLineWord();
        final int nextOffset = stream.position();
        if (line == null) {
          return -1;
        }
        nextLineCache.setNextLineOffset(filename, offset, nextOffset);
        if (line.contains(substring)) {
          return offset;
        }
        offset = nextOffset;
      } while (true);
    }
  }

  /**
   * {@inheritDoc}
   */
  // used by prefix search iterator
  public int getPrefixMatchLinePointer(int offset, final CharSequence prefix, final String filename) throws IOException {
    if (prefix.length() == 0) {
      return -1;
    }
    final int foffset = getIndexedLinePointer(prefix, offset, filename, true);
    final int zoffset;
    if (foffset < 0) {
      // invert -(o - 1)
      final int moffset = -(foffset + 1);
      final String aline = readLineAt(moffset, filename);
      if (aline == null || false == Utils.startsWith(aline, prefix)) {
        zoffset = foffset;
      } else {
        zoffset = moffset;
      }
    } else {
      zoffset = foffset;
    }
    return zoffset;
  }

  /**
   * {@inheritDoc}
   * XXX old version only languishing to verify new version
   */
  // used by prefix search iterator
  int oldGetPrefixMatchLinePointer(int offset, final CharSequence prefix, final String filename) throws IOException {
    if (prefix.length() == 0) {
      return -1;
    }
    final CharStream stream = getFileStream(filename);
    final int origOffset = offset;
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String line = stream.readLineWord();
        final int nextOffset = stream.position();
        if (line == null) {
          return -1;
        }
        nextLineCache.setNextLineOffset(filename, offset, nextOffset);
        if (Utils.startsWith(line, prefix)) {
          if (false == checkPrefixBinarySearch(prefix, origOffset, filename)) {
            throw new IllegalStateException("search failed for prefix: "+prefix+" filename: "+filename);
          }

          return offset;
        }
        offset = nextOffset;
      } while (true);
    }
  }

  // throw-away test method until confidence in binary-search based version gets near 100%
  private boolean checkPrefixBinarySearch(final CharSequence prefix, final int offset, final String filename) throws IOException {
    final int foffset = getIndexedLinePointer(prefix, offset, filename, true);
    //XXX System.err.println("foffset: "+foffset+" prefix: \""+prefix+"\"");
    final String aline;
    int zoffset;
    if (foffset < 0) {
      // invert -(o - 1)
      final int moffset = -(foffset + 1);
      zoffset = moffset;
      // if moffset < size && line[moffset].startsWith(prefix)
      aline = readLineAt(moffset, filename);
    } else {
      aline = readLineAt(foffset, filename);
      zoffset = foffset;
    }
    //XXX System.err.println("aline: \""+aline+"\" zoffset: "+zoffset);

    //System.err.println("line:  \""+line+"\" filename: "+filename);

    //if (aline != null && aline.startsWith(prefix)) {
    //  //assert offset >= 0;
    //  System.err.println("offset >= 0: "+(offset >= 0)+" prefix: \""+prefix+"\"");
    //} else {
    //  //assert offset < 0;
    //  System.err.println("offset < 0: "+(offset < 0)+" prefix: \""+prefix+"\"");
    //}
    //System.err.println();
    return aline != null && Utils.startsWith(aline, prefix);
  }

  /**
   * {@inheritDoc}
   */
  public int getIndexedLinePointer(final CharSequence target, final String filename) throws IOException {
    return getIndexedLinePointer(target, 0, filename, true);
  }

  /**
   * {@inheritDoc}
   */
  public int getIndexedLinePointer(final CharSequence target, int start, final String filename, final boolean filenameWnRelative) throws IOException {
    // This binary search method should be usable by prefix search changing it
    // from linear time to logarithmic time.
    //
    // - are there counter cases where the first-word binary search would return a different
    //   result than a "normal" binary search?
    //   - underscore comes before all lower cased letters
    if (target.length() == 0) {
      return -1;
    }
    if (log.isLoggable(Level.FINEST)) {
      log.finest("target: "+target+" filename: "+filename);
    }
    final CharStream stream = getFileStream(filename, filenameWnRelative);
    if (stream == null) {
      throw new IllegalArgumentException("no stream for "+filename);
    }
    synchronized (stream) {
      int stop = stream.length();
      while (true) {
        //FIXME fix possible overflow issue  with >>>
        final int midpoint = (start + stop) / 2;
        stream.seek(midpoint);
        stream.skipLine();
        final int offset = stream.position();
        if (log.isLoggable(Level.FINEST)) {
          log.finest("  "+start+", "+midpoint+", "+stop+" -> "+offset);
        }
        if (offset == start) {
          // cannot be a match here - would be zero width
          return -start - 1;
        } else if (offset == stop) {
          if (start != 0 && stream.charAt(start - 1) != '\n') {
            stream.seek(start + 1);
            stream.skipLine();
          } else {
            stream.seek(start);
          }
          if (log.isLoggable(Level.FINEST)) {
            log.finest(". "+stream.position());
          }
          //FIXME why is this a while() loop and not an if?
          // - scan through short lines?
          while (stream.position() < stop) {
            final int result = stream.position();
            final CharSequence firstWord = stream.readLineWord();
            if (log.isLoggable(Level.FINEST)) {
              log.finest("  . \""+firstWord+"\" -> "+(0 == compare(target, firstWord)));
            }
            final int compare = compare(target, firstWord);
            if (compare == 0) {
              return result;
            } else if (compare < 0) {
              return -result - 1;
            }
          }
          return -stop - 1;
        } // end offset == stop branch
        final int result = stream.position();
        final CharSequence firstWord = stream.readLineWord();
        final int compare = compare(target, firstWord);
        if (log.isLoggable(Level.FINEST)) {
          log.finest(firstWord + ": " + compare);
        }
        if (compare == 0) {
          return result;
        }
        if (compare > 0) {
          start = offset;
        } else {
          assert compare < 0;
          stop = offset;
        }
      }
    }
  }

  private static int compare(final CharSequence s1, final CharSequence s2) {
    return Utils.WordNetLexicalComparator.TO_LOWERCASE_INSTANCE.compare(s1, s2);
  }
}
