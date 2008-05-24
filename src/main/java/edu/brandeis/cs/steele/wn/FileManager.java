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

/** An implementation of <code>FileManagerInterface</code> that reads files from the local file system.
 * A file  <code>FileManager</code> caches the file position before and after
 * <code>readLineAt</code> in order to eliminate the redundant IO activity that a naive implementation
 * of these methods would necessitate.
 *
 * Instances of this class are guarded.  Operations are synchronized by file.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class FileManager implements FileManagerInterface {
  //
  // Class variables
  //
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
    // FIXME see notes in getWNSearchDir()
    String home = System.getProperty("WNHOME");
    if (home != null && new File(home).exists()) {
      return home;
    } else {
      home = System.getenv("WNHOME");
      if(home != null && new File(home).exists()) {
        return home;
      }
    }
    log.log(Level.SEVERE, "WNHOME is not defined correctly as either a Java system property or environment variable. "+
        System.getenv()+" \n\nsystem properties: "+System.getProperties());
    throw new IllegalStateException("WNHOME is not defined correctly as either a Java system property or environment variable. "+
        System.getenv()+" \n\nsystem properties: "+System.getProperties());
  }

  static String getWNSearchDir() {
    //FIXME unify logic for this and getWNSearchDir() to try both
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
      if(ret == null) {
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
      for(int i = 0; i < linenum; i++) {
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
    @Override int length() throws IOException {
      return (int) raf.length();
    }
    @Override String readLine() throws IOException {
      return raf.readLine();
    }
  } // end class RAFCharStream

  private static class NIOCharStream extends CharStream {
    //FIXME position seems redundant
    protected int position;
    //protected final ByteBuffer buf;
    protected final ByteCharBuffer buf;
    
    NIOCharStream(final RandomAccessFile raf) throws IOException {
      final FileChannel fileChannel = raf.getChannel();
      final MappedByteBuffer mmap = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
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
    @Override int length() throws IOException {
      return buf.capacity();
    }
    @Override String readLine() throws IOException {
      final int s = position;
      final int e = scanToLineBreak();
      assert s >= 0;
      assert e >= 0;
      final int len = e - s;
      if(len <= 0) {
        return null;
      }
      final char[] line = new char[len];
      for(int j = s, i = 0; i < line.length; ++i, ++j) {
        // NOTE: casting byte to char here since WordNet is currently
        // only ASCII
        line[i] = (char) buf.get(j);
      }
      final String toReturn = new String(line);
      return toReturn;
    }
    @Override void skipLine() throws IOException {
      scanToLineBreak();
    }
    @Override String readLineWord() throws IOException {
      final int s = position;
      int e = scanToLineBreak();
      assert s >= 0;
      assert e >= 0;
      int len = e - s;
      if(len <= 0) {
        return null;
      }
      e = scanToSpace(s);
      len = e - s;
      final char[] word = new char[len];
      for(int j = s, i = 0; i < word.length; ++i, ++j) {
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
      while(s < buf.capacity()) {
        c = (char) buf.get(s++);
        if(c == ' ') {
          return s - 1;
        }
      }
      throw new IllegalStateException();
    }
    protected int scanToLineBreak() {
      // scan from current position to first ("\r\n"|"\r"|"\n")
      boolean done = false;
      boolean crnl = false;
      char c;
      while(done == false && position < buf.capacity()) {
        c = (char) buf.get(position++);
        switch(c) {
          case '\r':
            // if next is \n, skip that too
            c = (char) buf.get(position++);
            if(c != '\n') {
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
  } // end class NIOCharStream

  /**
   * Like a read-only CharBuffer made from a ByteBuffer with a stride of 1
   * instead of 2.
   */
  private static class ByteCharBuffer implements CharSequence {
    private final ByteBuffer bb;
    ByteCharBuffer(final ByteBuffer bb) {
      this(bb, true);
    }
    ByteCharBuffer(final ByteBuffer bb, final boolean dupAndClear) {
      if(dupAndClear) {
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

  synchronized CharStream getFileStream(String filename) throws IOException {
    CharStream stream = filenameCache.get(filename);
    if (stream == null) {
      final String pathname = searchDirectory + File.separator + filename;
      final File file = new File(pathname);
      if(file.exists() && file.canRead()) {
        //slow CharStream
        //stream = new RAFCharStream(new RandomAccessFile(pathname, "r"));
        //fast CharStream stream
        stream = new NIOCharStream(new RandomAccessFile(file, "r"));
      }
      filenameCache.put(filename, stream);
    }
    return stream;
  }

  //
  // Line-based interface methods
  //
  
  public String readLineNumber(final String filename, final int linenum) throws IOException {
    final CharStream stream = getFileStream(filename);
    if(stream == null) {
      return null;
    }
    synchronized (stream) {
      return stream.readLineNumber(linenum);
    }
  }

  // only called from within synchronized blocks
  // core search routine
  public String readLineAt(final String filename, final int offset) throws IOException {
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

  // only called from within synchronized blocks
  // core search routine
  public int getNextLinePointer(final String filename, final int offset) throws IOException {
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

  // used by substring search iterator
  public int getMatchingLinePointer(final String filename, int offset, final String substring) throws IOException {
    if(substring.length() == 0) {
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
        if (line.indexOf(substring) >= 0) {
          return offset;
        }
        offset = nextOffset;
      } while (true);
    }
  }

  // used by prefix search iterator
  public int getMatchingBeginningLinePointer(final String filename, int offset, final String prefix) throws IOException {
    //TODO test if prefix is empty string
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
        if (line.startsWith(prefix)) {
          return offset;
        }
        offset = nextOffset;
      } while (true);
    }
  }
  
  /** Binary search for line from file implied by <param>filename</param> which starts with 
   * a word equal to <param>target</param>.  Assumes this file is sorted by its first
   * textual column of lowercased words.  This condtion can be verified with UNIX sort
   * with the command <tt>sort -k1,1 -c</tt>
   */
  public int getIndexedLinePointer(final String filename, final String target) throws IOException {
    if (target.length() == 0) {
      return -1;
    }
    if (log.isLoggable(Level.FINEST)) {
      log.finest("target: "+target+" filename: "+filename);
    }
    final CharStream stream = getFileStream(filename);
    if(stream == null) {
      return -1;
    }
    synchronized (stream) {
      int start = 0;
      int stop = stream.length();
      while (true) {
        final int midpoint = (start + stop) / 2;
        stream.seek(midpoint);
        stream.skipLine();
        final int offset = stream.position();
        if (log.isLoggable(Level.FINEST)) {
          log.finest("  "+start+", "+midpoint+", "+stop+" -> "+offset);
        }
        if (offset == start) {
          // cannot be a match here - would be zero width
          return -1;
        } else if (offset == stop) {
          if(start != 0) {
            stream.seek(start + 1);
            stream.skipLine();
          } else {
            stream.seek(start /* 0 */);
          }
          if (log.isLoggable(Level.FINEST)) {
            log.finest(". "+stream.position());
          }
          //FIXME why is this a while() loop and not an if??
          //the stream position is not being updated in this loop
          while (stream.position() < stop) {
            final int result = stream.position();
            final CharSequence firstWord = stream.readLineWord();
            if (log.isLoggable(Level.FINEST)) {
              log.finest("  . \""+firstWord+"\" -> "+target.contentEquals(firstWord));
            }
            if (target.contentEquals(firstWord)) {
              return result;
            }
          }
          return -1;
        } // end offset == stop branch
        final int result = stream.position();
        final CharSequence firstWord = stream.readLineWord();
        final int compare = CharSequenceComparator.INSTANCE.compare(target, firstWord);
        if (log.isLoggable(Level.FINEST)) {
          log.finest(firstWord + ": " + compare);
        }
        if (compare == 0) return result;
        if (compare > 0) {
          start = offset;
        } else {
          assert compare < 0;
          stop = offset;
        }
      }
    }
  }
  
  // generic comparator for CharSequence / String pairs
  static class CharSequenceComparator implements Comparator, Serializable {
    private static final long serialVersionUID = 1L;
    
    public int compare(final Object o1, final Object o2) {
      if(o1 instanceof String && o2 instanceof String) {
        return ((String)o1).compareTo((String)o2);
      } else if(o1 instanceof CharSequence && 
          o2 instanceof CharSequence) {
        final CharSequence s1 = (CharSequence) o1;
        final CharSequence s2 = (CharSequence) o2;
        int i = 0;
        int n = Math.min(s1.length(), s2.length());
        while (n-- != 0) {
          final char c1 = s1.charAt(i);
          final char c2 = s2.charAt(i++);
          if (c1 != c2) {
            return c1 - c2;
          }
        }
        return s1.length() - s2.length();
      } else {
        throw new IllegalArgumentException();
      }
    }
    public boolean equals(Object obj) {
      return obj instanceof CharSequenceComparator;
    }
    public static final CharSequenceComparator INSTANCE = new CharSequenceComparator();
  } // end class CharSequenceComparator
}
