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
package org.yawni.wordnet;

import org.yawni.util.CharSequences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * An implementation of {@code FileManagerInterface} that reads WordNet data
 * from jar files or the local file system.  A {@code FileManager} caches the
 * file positions before and after {@link FileManagerInterface#readLineAt}
 * in order to eliminate the redundant I/O activity that a naïve implementation
 * of these methods would necessitate.
 *
 * <p> Instances of this class are guarded; all operations are read-only, but
 * are synchronized per file to maintain state including the file pointers'
 * position.
 */
final class FileManager implements FileManagerInterface {
  private static final Logger log = LoggerFactory.getLogger(FileManager.class.getName());

//  private String searchDirectory;
  private final Map<String, CharStream> filenameCache = new HashMap<String, CharStream>();

  static class NextLineOffsetCache {
    private String filename;
    private int previous;
    private int next;

    /**
     * synchronization keeps this consistent since multiple filename's may call
     * this at the same time
     */
    synchronized void setNextLineOffset(final String filename, final int previous, final int next) {
      this.filename = filename;
      this.previous = previous;
      this.next = next;
    }

    /**
     * synchronization keeps this consistent since multiple filename's may call
     * this at the same time
     */
    synchronized int matchingOffset(final String filename, final int offset) {
      if (this.filename == null ||
          previous != offset ||
          //! this.filename.equals(filename)
          ! this.filename.equals(filename)
          ) {
        return -1;
      } else {
        return next;
      }
    }
  } // end class NextLineOffsetCache
  private final NextLineOffsetCache nextLineOffsetCache = new NextLineOffsetCache();

  //
  // Constructors
  //
  /** FIXME
   * Construct a {@code FileManager} backed by a set of files contained in the default WordNet search directory.
   * The default search directory is the location named by the system property {@code $WNSEARCHDIR}; or, if this
   * is undefined, by the directory named {@code $WNHOME/dict}.
   */
  public FileManager() {
//    this(getWNSearchDir());
  }

  /** 
   * Construct a {@code FileManager} backed by a set of files contained in
   * {@code searchDirectory}.
   */
//  public FileManager(final String searchDirectory) {
//    this.searchDirectory = searchDirectory;
//  }

  /**
   * Directory which contains all WordNet data files defined by {@code $WNSEARCHDIR}.
   * The {@code WNsearchDir} is typically {@code $WNHOME/dict}
   * @see #getFileStream(String, boolean)
   */
  private static String getWNSearchDir() {
    final String searchDir = getValidatedPathNamed("WNSEARCHDIR");
    if (searchDir != null) {
      // searchDir better have our files in it or we're screwed
      // even if $WNHOME is correct!
      return searchDir;
    }
    // WNSEARCHDIR was not defined & readable - try generating it from WNHOME
    final String wnHome = getValidatedPathNamed("WNHOME");
    String generatedSearchDir = null;
    if (wnHome != null) {
      generatedSearchDir = wnHome + File.separator + "dict/";
      if (! isReadableFile(generatedSearchDir)) {
        generatedSearchDir = null;
      }
    }
    return generatedSearchDir;
  }

  /**
   * Searches an environment variable and then a Java System Property 
   * named {@code propname} and if its value refers to a readable file,
   * returns that path, otherwise returns {@code null}.
   */
  static String getValidatedPathNamed(final String propName) {
    try {
      String path;
      path = System.getenv(propName);
      if (isReadableFile(path)) {
        return path;
      } else {
        path = System.getProperty(propName);
        if (isReadableFile(path)) {
          return path;
        }
      }
    } catch (SecurityException ex) {
      log.debug("need plan B due to: {}", ex);
      return null;
    }
    //log.error(propName+" is not defined correctly as either a Java system property or environment variable. "+
    //    System.getenv()+" \n\nsystem properties: "+System.getProperties());
    //throw new IllegalStateException("WNHOME is not defined correctly as either a Java system property or environment variable. "+
    //    System.getenv()+" \n\nsystem properties: "+System.getProperties());
    return null;
  }

  static boolean isReadableFile(final String path) {
    File file;
    return path != null &&
      (file = new File(path)).exists() &&
      file.canRead();
  }

  //
  // I/O primitives
  //

  /**
   * Primary abstraction of file content used in {@code FileManager}.
   * NOTE: CharStream is stateful (i.e., not thread-safe)
   */
  static abstract class CharStream {
    protected final String filename;
    /** Force subclasses to call this */
    CharStream(final String filename) {
      this.filename = filename;
    }
    abstract void seek(final int position) throws IOException;
    abstract int position() throws IOException;
    abstract char charAt(int position) throws IOException;
    abstract int length() throws IOException;
    /**
     * This works just like {@link RandomAccessFile#readLine} -- doesn't
     * support Unicode.
     */
    abstract String readLine() throws IOException;
    void skipLine() throws IOException {
      readLine();
    }
    // reads line, returns first space delimited word
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
     * inclusive line corresponding to {@code linenum}
     */
    String readLineNumber(int linenum) throws IOException {
      //TODO when creating the CharStream, add option to "index"/cache these results as either String[] OR String[][]
      //where each row is an array of the delimited items on it and a second optional argument
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

  /**
   * {@link RandomAccessFile}-backed {@code CharStream} implementation.  This {@code CharStream}
   * has the minimum boot time (and the slowest access times).
   */
  static class RAFCharStream extends CharStream {
    private final RandomAccessFile raf;
    RAFCharStream(final String filename, final RandomAccessFile raf) {
      super(filename);
      this.raf = raf;
    }
    @Override
    void seek(final int position) throws IOException {
      raf.seek(position);
    }
    @Override
    int position() throws IOException {
      return (int) raf.getFilePointer();
    }
    @Override
    char charAt(int position) throws IOException {
      seek(position);
      return (char)raf.readByte();
    }
    @Override
    int length() throws IOException {
      return (int) raf.length();
    }
    @Override
    String readLine() throws IOException {
      return raf.readLine();
    }
  } // end class RAFCharStream

  /**
   * {@link ByteBuffer} {@code CharStream} implementation.
   * This {@code CharStream} is boots very quickly (little slower than
   * {@code RAFCharStream}) and provides very fast access times, however it
   * requires a {@code ByteBuffer} which is usually most easily derived
   * from an {@code FileChannel}. aka {@code mmap CharStream}
   */
  private static class NIOCharStream extends CharStream {
    //FIXME position seems redundant (ByteCharBuffer has position())
    private int position;
    private final ByteBuffer bbuff;
    //private final ByteCharBuffer bbuff;
    private final StringBuilder stringBuffer;
    private final int capacity;

    NIOCharStream(final String filename, final ByteBuffer bbuff) throws IOException {
      super(filename);
      this.bbuff = bbuff;
      this.capacity = bbuff.capacity();
      this.stringBuffer = new StringBuilder();
    }
    NIOCharStream(final String filename, final RandomAccessFile raf) throws IOException {
      this(filename, asByteBuffer(raf));
    }
    private static ByteBuffer asByteBuffer(final RandomAccessFile raf) throws IOException {
      final FileChannel fileChannel = raf.getChannel();
      final long size = fileChannel.size();
      // program logic currently depends on the entire file being mapped into memory
      // size /= 2;
      final MappedByteBuffer mmap = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
      // this buffer isDirect()
      //log.debug("mmap.fine(): {}", mmap.isDirect());
      //this.bbuff = new ByteCharBuffer(mmap, false);
      return mmap;
    }
    @Override
    void seek(final int position) throws IOException {
      // buffer cannot exceed Integer.MAX_VALUE since arrays are limited by this
      this.position = position;
    }
    @Override
    int position() throws IOException {
      return position;
    }
    @Override
    char charAt(final int p) throws IOException {
      return (char) bbuff.get(p);
    }
    @Override
    int length() throws IOException {
      return bbuff.capacity();
    }
    @Override
    String readLine() throws IOException {
      final int s = position;
      final int e = scanForwardToLineBreak(true);
      if ((e - s) <= 0) {
        return null;
      }
      return stringBuffer.toString();
    }
    @Override
    void skipLine() throws IOException {
      scanForwardToLineBreak();
    }
    @Override
    String readLineWord() throws IOException {
      final int s = position;
      bufferUntilSpace();
      final int e = scanForwardToLineBreak();
      if ((e - s) <= 0) {
        return null;
      }
      return stringBuffer.toString();
    }
    /** Modifies {@code position} field */
    private int bufferUntilSpace() {
      // scan from current position to first ' '
      resetBuffer(true /* doBuffer */);
      char c;
      while (position < capacity) {
        c = (char) bbuff.get(position++);
        if (c == ' ') {
          return position - 1;
        }
        stringBuffer.append(c);
      }
      return bbuff.capacity();
    }
    private int scanForwardToLineBreak() {
      return scanForwardToLineBreak(false /* don't buffer */);
    }
    /** Modifies {@code position} field */
    private int scanForwardToLineBreak(final boolean doBuffer) {
      // scan from current position to first ("\r\n"|"\r"|"\n")
      boolean done = false;
      boolean crnl = false;
      resetBuffer(doBuffer);
      char c;
      while (! done && position < capacity) {
        c = (char) bbuff.get(position++);
        switch (c) {
          case '\r':
            // if next is \n, skip that too
            c = (char) bbuff.get(position++);
            if (c != '\n') {
              // put it back
              position--;
            } else {
              crnl = true;
            }
            done = true;
            break;
          case '\n':
            done = true;
            break;
          default:
            if (doBuffer) {
              stringBuffer.append(c);
            }
        }
      }
      // return exclusive end chopping line break delimitter(s)
      return crnl ? position - 2 : position - 1;
    }
    private int scanBackwardToLineBreak() {
      // scan backwards to first \n
      // - if immediately preceding char is \n, keep going
      throw new UnsupportedOperationException();
    }
    private void resetBuffer(final boolean doBuffer) {
      if (doBuffer) {
        stringBuffer.setLength(0);
      }
    }
  } // end class NIOCharStream
  
  /**
   * Fast {@code CharStream} created from InputStream (e.g., can be read from jar file)
   * backed by a byte[].  This {@code CharStream} is slowest to boot
   * but provides very fast access times.
   */
  private static class InputStreamCharStream extends NIOCharStream {
    /**
     * @param filename interpretted as classpath relative path
     * @param input
     * @param len the number of bytes in this input stream.  Allows stream to be drained into exactly 
     * 1 buffer thus maximizing efficiency.
     */
    InputStreamCharStream(final String filename, final InputStream input, final int len) throws IOException {
      super(filename, asByteBuffer(input, len, filename));
    }
//    /**
//     * @param filepath
//     */
//    InputStreamCharStream(final String filepath) throws IOException {
//      this(filepath, new FileInputStream(filepath), -1);
//    }
    /**
     * @param input
     * @param len the number of bytes in this input stream.  Allows stream to be drained into exactly 
     * 1 buffer thus maximizing efficiency.
     * @param filename 
     */
    private static ByteBuffer asByteBuffer(final InputStream input, final int len, final String filename) throws IOException {
      if (len == -1) {
        throw new RuntimeException("unknown length not currently supported");
      }
      final byte[] buffer = new byte[len];
      int totalBytesRead = 0;
      int bytesRead;
      while ((bytesRead = input.read(buffer, totalBytesRead, len - totalBytesRead)) > 0) {
        totalBytesRead += bytesRead;
      }
      // could resize buffer
      if (len != totalBytesRead) {
        throw new RuntimeException("Read error. Only read "+totalBytesRead+" of "+len+" for "+filename);
      }
      return ByteBuffer.wrap(buffer);
    }
  } // end class InputStreamCharStream

  private long streamInitTime;

  /**
   * @param filename
   * @param filenameIsWnRelative is a boolean which indicates that {@code filename}
   * is relative (else, it's absolute); this facilitates testing and reuse.
   * @return CharStream representing {@code filename} or null if no such file exists.
   */
  private synchronized CharStream getFileStream(final String filename, final boolean filenameIsWnRelative) throws IOException {
    CharStream stream = filenameCache.get(filename);
    if (stream == null) {
      final long start = System.nanoTime();

      stream = getURLStream(filename);
      if (stream != null) {
        log.trace("URLCharStream: {}", stream);
      } else {
        final String pathname =
          filenameIsWnRelative ?
            getWNSearchDir() + File.separator + filename :
            filename;
        log.trace("filename: {} pathname: {}", filename, pathname);

        final File file = new File(pathname);
        log.debug("pathname: {}", pathname);
        if (file.exists() && file.canRead()) {
          // TODO make this config selectable ? unfortunately, other than init time,
          // performance of RAFCharStream is horrible
          
          //slow CharStream
          //stream = new RAFCharStream(pathname, new RandomAccessFile(pathname, "r"));
          //fast CharStream stream
          stream = new NIOCharStream(pathname, new RandomAccessFile(file, "r"));
          log.trace("FileCharStream");
        }
      }
      
      final long duration = System.nanoTime() - start;
      final long total = streamInitTime += duration;
      log.debug(String.format("total: %,dns curr: %,dns", total, duration));
//      assert stream != null : "stream is still null";
//      if (stream == null) {
//        return null;
//      }
      filenameCache.put(filename, stream);
    }
    
    return stream;
  }

  synchronized CharStream getFileStream(final String filename) throws IOException {
    return getFileStream(filename, true);
  }

  /**
   * Interpret {@code resourcename} as a classpath-relative URL.
   * @param resourcename
   * @return CharStream corresponding to resourcename
   */
  private synchronized CharStream getURLStream(String resourcename) throws IOException {
    resourcename = "dict/" + resourcename;
    // assume WN dict/ is in the classpath
    final URL url = getClass().getClassLoader().getResource(resourcename);
    if (url == null) {
      log.debug("resourcename: {} not found in classpath", resourcename);
      return null;
    }
    final URLConnection conn = url.openConnection();
    // get resource length so we can avoid unnecessary buffer copies
    final int len;
    if (conn instanceof JarURLConnection) {
      // JarURLConnection.getContentLength() returns the raw size of the source
      // jar file rather than the uncompressed entry's size if it is a different
      // jar from this class's definition
      final JarURLConnection juc = (JarURLConnection)conn;
      len = (int) juc.getJarEntry().getSize();
    } else {
      len = conn.getContentLength();
    }
    final InputStream input = conn.getInputStream();
    // fast CharStream created from InputStream (e.g., could be read from jar file)
    return new InputStreamCharStream(resourcename, input, len);
  }

  private void requireStream(final CharStream stream, final String filename) {
    if (stream == null) {
      throw new IllegalStateException("Yawni can't open '"+filename+
        "'. Yawni needs either the yawni-data jar in the classpath, or correctly defined " +
        " $WNSEARCHDIR or $WNHOME environment variable or system property referencing the WordNet data.");
    }
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
   * Core search routine.  Only called from within synchronized blocks.
   */
  public String readLineAt(final int offset, final String filename) throws IOException {
    final CharStream stream = getFileStream(filename);
    requireStream(stream, filename);
    synchronized (stream) {
      stream.seek(offset);
      final String line = stream.readLine();

      int nextOffset = stream.position();
      if (line == null) {
        nextOffset = -1;
      }
      nextLineOffsetCache.setNextLineOffset(filename, offset, nextOffset);
      return line;
    }
  }

  /**
   * {@inheritDoc}
   * Core search routine.  Only called from within synchronized blocks.
   */
  public int getNextLinePointer(final int offset, final String filename) throws IOException {
    final CharStream stream = getFileStream(filename);
    requireStream(stream, filename);
    synchronized (stream) {
      final int next;
      if (0 <= (next = nextLineOffsetCache.matchingOffset(filename, offset))) {
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

  private static final String TWO_SPACES = "  ";
  
  /**
   * {@inheritDoc}
   */
  // used by substring search iterator
  public int getMatchingLinePointer(int offset, final Matcher matcher, final String filename) throws IOException {
    if (matcher.pattern().pattern().length() == 0) {
      // shunt behavior where empty string matches everything
      // assert "anything".matches("");
      // assert "anything".contains("");
      return -1;
    }
    final CharStream stream = getFileStream(filename);
    requireStream(stream, filename);
    synchronized (stream) {
      stream.seek(offset);
      do {
        final String word = stream.readLineWord();
        final int nextOffset = stream.position();
        if (word == null) {
          return -1;
        }
        nextLineOffsetCache.setNextLineOffset(filename, offset, nextOffset);
        // note the spaces of this 'word' are underscores
        if (matcher.reset(word).find()) {
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
      if (aline == null || ! CharSequences.startsWith(aline, prefix)) {
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
        // note the spaces of this 'word' are underscores
        final String word = stream.readLineWord();
        final int nextOffset = stream.position();
        if (word == null) {
          return -1;
        }
        nextLineOffsetCache.setNextLineOffset(filename, offset, nextOffset);
        if (CharSequences.startsWith(word, prefix)) {
          if (! checkPrefixBinarySearch(prefix, origOffset, filename)) {
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
    //int zoffset;
    if (foffset < 0) {
      // invert -(o - 1)
      final int moffset = -(foffset + 1);
      //zoffset = moffset;
      // if moffset < size && line[moffset].startsWith(prefix)
      aline = readLineAt(moffset, filename);
    } else {
      aline = readLineAt(foffset, filename);
      //zoffset = foffset;
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
    return aline != null && CharSequences.startsWith(aline, prefix);
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
    // This binary search method provides output usable by prefix search
    // changing this operation from linear time to logarithmic time.
    //
    // - are there counter examples where the first-word binary search would return a different
    //   result than a "normal" binary search?
    //   - underscore comes before all lower cased letters
    //assert ! Utils.containsUpper(target);
    if (target.length() == 0) {
      return -1;
    }
    if (log.isTraceEnabled()) {
      log.trace("target: "+target+" filename: "+filename);
    }
    final CharStream stream = getFileStream(filename, filenameWnRelative);
    requireStream(stream, filename);
    synchronized (stream) {
      int stop = stream.length();
      while (true) {
        //FIXME fix possible overflow issue; fix with >>>
        final int midpoint = (start + stop) / 2;
        stream.seek(midpoint);
        stream.skipLine();
        final int offset = stream.position();
        if (log.isTraceEnabled()) {
          log.trace("  "+start+", "+midpoint+", "+stop+" → "+offset);
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
          if (log.isTraceEnabled()) {
            log.trace(". "+stream.position());
          }
          //FIXME why is this a while() loop and not an if?
          // - scan through short lines?
          while (stream.position() < stop) {
            final int result = stream.position();
            // note the spaces of this 'word' are underscores
            final CharSequence word = stream.readLineWord();
            if (log.isTraceEnabled()) {
              log.trace("  . \""+word+"\" → "+(0 == compare(target, word)));
            }
            final int compare = compare(target, word);
            if (compare == 0) {
              return result;
            } else if (compare < 0) {
              return -result - 1;
            }
          }
          return -stop - 1;
        } // end offset == stop branch
        final int result = stream.position();
        final CharSequence word = stream.readLineWord();
        final int compare = compare(target, word);
        if (log.isTraceEnabled()) {
          log.trace(word + ": " + compare);
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

  /**
   * {@inheritDoc}
   * Note this is a covariant implementation of {@link java.util.Comparator Comparator<CharSequence>}
   */
  public WordNetLexicalComparator comparator() {
    // caseless searches rely on this
    return WordNetLexicalComparator.TO_LOWERCASE_INSTANCE;
    //return Utils.WordNetLexicalComparator.GIVEN_CASE_INSTANCE;
  }

  private int compare(final CharSequence s1, final CharSequence s2) {
    return comparator().compare(s1, s2);
  }
}