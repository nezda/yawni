/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.*;
import java.util.logging.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/** An implementation of FileManagerInterface that reads files from the local file system.
 * a file.  <code>FileManager</code> caches the file position before and after
 * <code>readLineAt</code> in order to eliminate the redundant IO activity that a naive implementation
 * of these methods would necessitate.
 *
 * Instances of this class are guarded.  Operations are synchronized by file.
 *
 * TODO complete tagged sense count by parsing this file
 * // The name of the file that contain word sense frequency information.
 * protected static final String frequencyFile = "/dict/cntlist";
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
  public static String VERSION = "1.5.0";

  /** Set this to true to enable debugging messages in <code>getIndexedLinePointer</code>. */
  public static final boolean TRACE_LOOKUP = false; // unused

  //
  // Filename caching
  //
  protected static final boolean IS_WINDOWS_OS = System.getProperty("os.name").startsWith("Windows");
  protected static final boolean IS_MAC_OS = false; //System.getProperty("os.name").startsWith("Mac");

  //
  // Instance variables
  //
  protected String searchDirectory;
  protected Map<String, CharStream> filenameCache = new HashMap<String, CharStream>();

  protected static class NextLineCache {
    protected String filename;
    protected int previous;
    protected int next;

    void setNextLineOffset(String filename, int previous, int next) {
      this.filename = filename;
      this.previous = previous;
      this.next = next;
    }

    boolean matchingOffset(String filename, int offset) {
      //FIXME XXX HACK HACK DISABLING
      if(true) return false;
      return this.filename != null && previous == offset && this.filename.equals(filename);
    }

    int getNextOffset() {
      return next;
    }
  }
  protected NextLineCache nextLineCache = new NextLineCache();

  //
  // Constructors
  //
  /** Construct a file manager backed by a set of files contained in the default WordNet search directory.
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

  protected static String getWNHome() {
    String home = System.getProperty("WNHOME");
    if (home != null) {
      return home;
    } else {
      home = System.getenv("WNHOME");
      if(home != null) {
        return home;
      }
    }
    throw new IllegalStateException("WNHOME is undefined in both Java System properties AND environment. "+
        System.getenv());
  }

  protected static String getWNSearchDir() {
    final String searchDir = System.getProperty("WNSEARCHDIR");
    if (searchDir != null) {
      return searchDir;
    } else if (IS_MAC_OS && getWNHome().equals(".")) {
      return "Database";
    } else {
      return getWNHome() + File.separator + (IS_MAC_OS ? "Database" : "dict");
    }
  }

  static String mapToWindowsFilename(String filename) {
    if (filename.startsWith("data.")) {
      filename = filename.substring("data.".length()) + ".dat";
    } else if (filename.startsWith("index.")) {
      filename = filename.substring("index.".length()) + ".idx";
    }
    return filename;
  }

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
  } // end class CharStream

  static class RAFCharStream extends CharStream {
    private RandomAccessFile raf;
    RAFCharStream(final RandomAccessFile raf) {
      this.raf = raf;
    }
    @Override void seek(final int position) throws IOException {
      raf.seek(position);
    }
    @Override int position() throws IOException {
      // this application doesn't require longs
      return (int) raf.getFilePointer();
    }
    @Override int length() throws IOException {
      // this application doesn't require longs
      return (int)raf.length();
    }
    @Override String readLine() throws IOException {
      return raf.readLine();
    }
  } // end class RAFCharStream

  static class NIOCharStream extends CharStream {
    // TODO switch from absolute get methods to relative methods (don't know
    // how right now)
    private int position;
    private ByteBuffer buf;
    NIOCharStream(final RandomAccessFile raf) throws IOException {
      // optionally, mmap this file
      final FileChannel fileChannel = raf.getChannel();
      final MappedByteBuffer mmap = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
      this.buf = mmap;
    }
    @Override void seek(final int position) throws IOException {
      // buffer cannot exceed Integer.MAX_VALUE since arrays are limited by this
      this.position = (int) position;
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
      int len = e - s;
      if(len <= 0) {
        return null;
      }
      final char[] line = new char[len];
      for(int j = s, i = 0; i < line.length; ++i, ++j) {
        line[i] = (char) buf.get(j);
      }
      final String toReturn = new String(line);
      //System.err.println("returning: \""+toReturn+"\"");
      return toReturn;
    }
    @Override void skipLine() throws IOException {
      scanToLineBreak();
    }
    private int scanToLineBreak() {
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

  protected synchronized CharStream getFileStream(String filename) throws IOException {
    if (IS_WINDOWS_OS) {
      //TODO would be slow on Windows
      filename = mapToWindowsFilename(filename);
    }
    CharStream stream = filenameCache.get(filename);
    if (stream == null) {
      final String pathname = searchDirectory + File.separator + filename;
      //slow CharStream? TODO test
      //stream = new RAFCharStream(new RandomAccessFile(pathname, "r"));
      //fast CharStream stream? TODO test
      stream = new NIOCharStream(new RandomAccessFile(pathname, "r"));
      filenameCache.put(filename, stream);
    }
    return stream;
  }

  //
  // IO primitives
  //

  // only called from within synchronized blocks
  protected String readLine(final CharStream stream) throws IOException {
    return stream.readLine();
  }

  // only called from within synchronized blocks
  protected void skipLine(final CharStream stream) throws IOException {
    stream.skipLine();
  }

  //
  // Line-based interface methods
  //
  public String readLineAt(final String filename, final int offset) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      final String line = readLine(stream);

      int nextOffset = stream.position();
      if (line == null) {
        nextOffset = -1;
      }
      nextLineCache.setNextLineOffset(filename, offset, nextOffset);
      return line;
    }
  }

  // only called from within synchronized blocks
  protected String readLineWord(final CharStream stream) throws IOException {
    final String ret = stream.readLine();
    if(ret == null) {
      return null;
    }
    // LN added new to leak less String memory here
    final int space = ret.indexOf(' ');
    assert space >= 0;
    String word = new String(ret.substring(0, space));
    return word;
  }

  public int getNextLinePointer(final String filename, final int offset) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      if (nextLineCache.matchingOffset(filename, offset)) {
        return nextLineCache.getNextOffset();
      }
      stream.seek(offset);
      skipLine(stream);
      return stream.position();
    }
  }

  //
  // Low-level Searching
  //
  public int getMatchingLinePointer(final String filename, int offset, final String substring) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String line = readLineWord(stream);
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

  public int getMatchingBeginningLinePointer(final String filename, int offset, final String prefix) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String line = readLineWord(stream);
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
  
  /** Binary search line implied by <param>filename</param> for <param>target</param>. */
  public int getIndexedLinePointer(final String filename, final String target) throws IOException {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("target:"+target);
      log.finest("filename:"+filename);
    }
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      int start = 0;
      int stop = stream.length();
      while (true) {
        final int midpoint = (start + stop) / 2;
        stream.seek(midpoint);
        skipLine(stream);
        final int offset = stream.position();
        if (log.isLoggable(Level.FINEST)) {
          log.finest("  "+start+", "+((start+stop)/2)+", "+stop+" -> "+offset);
        }
        if (offset == start) {
          return -1;
        } else if (offset == stop) {
          stream.seek(start + 1);
          skipLine(stream);
          if (log.isLoggable(Level.FINEST)) {
            log.finest(". "+stream.position());
          }
          while (stream.position() < stop) {
            final int result = stream.position();
            final String line = readLineWord(stream);
            if (log.isLoggable(Level.FINEST)) {
              log.finest(". "+line+" -> "+line.equals(target));
            }
            if (line.equals(target)) {
              return result;
            }
          }
          return -1;
        }
        final int result = stream.position();
        final String line = readLineWord(stream);
        if (line.equals(target)) return result;
        final int compare = target.compareTo(line);
        if (log.isLoggable(Level.FINEST)) {
          log.finest(line + ": " + compare);
        }
        if (compare > 0) {
          start = offset;
        } else if (compare < 0) {
          stop = offset;
        } else {
          return result;
        }
      }
    }
  }
}
