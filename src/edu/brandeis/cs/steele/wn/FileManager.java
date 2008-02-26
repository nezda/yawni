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
import java.nio.charset.*;
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
    protected long previous;
    protected long next;

    void setNextLineOffset(String filename, long previous, long next) {
      this.filename = filename;
      this.previous = previous;
      this.next = next;
    }

    boolean matchingOffset(String filename, long offset) {
      //FIXME XXX HACK HACK DISABLING
      if(true) return false;
      return this.filename != null && previous == offset && this.filename.equals(filename);
    }

    long getNextOffset() {
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
    abstract void seek(final long position) throws IOException;
    abstract long position() throws IOException;
    abstract long length() throws IOException;
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
    @Override void seek(final long position) throws IOException {
      raf.seek(position);
    }
    @Override long position() throws IOException {
      return raf.getFilePointer();
    }
    @Override long length() throws IOException {
      return raf.length();
    }
    @Override String readLine() throws IOException {
      return raf.readLine();
    }
  } // end class RAFCharStream

  static class NIOCharStream extends CharStream {
    protected int position;
    protected final ByteBuffer buf;
    
    NIOCharStream(final RandomAccessFile raf) throws IOException {
      final FileChannel fileChannel = raf.getChannel();
      final MappedByteBuffer mmap = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
      // this buffer isDirect()
      //log.log(Level.FINE, "mmap.fine(): {0}", mmap.isDirect());
      this.buf = mmap;
    }
    @Override void seek(final long position) throws IOException {
      // buffer cannot exceed Integer.MAX_VALUE since arrays are limited by this
      this.position = (int) position;
    }
    @Override long position() throws IOException {
      return position;
    }
    @Override long length() throws IOException {
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

  static class NIOCharStream2 extends NIOCharStream {
    private CharBuffer cbuf;
    private final CharsetDecoder decoder;
    
    NIOCharStream2(final RandomAccessFile raf) throws IOException {
      super(raf);
      this.cbuf = CharBuffer.allocate(1024);
      final Charset US_ASCII = Charset.forName(/*"US-ASCII"*/"ISO-8859-1");
      this.decoder = US_ASCII.newDecoder();
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
      buf.position(s);
      buf.limit(e);
      cbuf = resize(cbuf, len);
      cbuf.clear();
      decoder.reset();
      CoderResult coderResult = decoder.decode(buf, cbuf, false);
      assert coderResult == CoderResult.UNDERFLOW;
      coderResult = decoder.decode(buf, cbuf, true);
      assert coderResult == CoderResult.UNDERFLOW;
      coderResult = decoder.flush(cbuf);
      assert coderResult == CoderResult.UNDERFLOW;
      cbuf.flip();
      final String nioline = cbuf.toString();
      buf.clear();
      return nioline;
    }
    private static CharBuffer resize(final CharBuffer cbuf, final int len) {
      if(len <= cbuf.capacity()) {
        return cbuf;
      }
      final CharBuffer newBuf = CharBuffer.allocate(Math.max(len, cbuf.capacity() * 2));
      cbuf.flip();
      newBuf.put(cbuf);
      return newBuf;
    }
  } // end class NIOCharStream2

  protected synchronized CharStream getFileStream(String filename) throws IOException {
    if (IS_WINDOWS_OS) {
      //TODO would be slow on Windows
      filename = mapToWindowsFilename(filename);
    }
    CharStream stream = filenameCache.get(filename);
    if (stream == null) {
      final String pathname = searchDirectory + File.separator + filename;
      //slow CharStream
      //stream = new RAFCharStream(new RandomAccessFile(pathname, "r"));
      //fast CharStream stream
      stream = new NIOCharStream(new RandomAccessFile(pathname, "r"));
      //not as fast as NIOCharStream
      //stream = new NIOCharStream2(new RandomAccessFile(pathname, "r"));
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
  public String readLineAt(final String filename, final long offset) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      final String line = readLine(stream);

      long nextOffset = stream.position();
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

  public long getNextLinePointer(final String filename, final long offset) throws IOException {
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
  public long getMatchingLinePointer(final String filename, long offset, final String substring) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String line = readLineWord(stream);
        final long nextOffset = stream.position();
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

  public long getMatchingBeginningLinePointer(final String filename, long offset, final String prefix) throws IOException {
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String line = readLineWord(stream);
        final long nextOffset = stream.position();
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
  public long getIndexedLinePointer(final String filename, final String target) throws IOException {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("target:"+target);
      log.finest("filename:"+filename);
    }
    final CharStream stream = getFileStream(filename);
    synchronized (stream) {
      long start = 0;
      long stop = stream.length();
      while (true) {
        final long midpoint = (start + stop) / 2;
        stream.seek(midpoint);
        skipLine(stream);
        final long offset = stream.position();
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
            final long result = stream.position();
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
        final long result = stream.position();
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
