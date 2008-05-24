/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** <code>FileManagerInterface</code> defines the interface between the <code>FileBackedDictionary</code> and the file system.
 * <code>FileBackedDictionary</code> invokes methods from this interface to retrieve lines of text from the
 * WordNet data files.
 * 
 * <p>Methods in this interface take filenames as arguments.  The filename is the name of
 * a WordNet file, and is relative to the database directory (e.g. <code>data.noun</code>, not
 * <code>dict/data.noun</code>).
 * 
 * <p>Methods in this interface operate on and return pointers, which are indices into the
 * file named by filename.
 * 
 * <p><code>FileManagerInterface</code> is designed to work efficiently across a network.  To this end, it obeys
 * two design principles:  it uses only primitive types (including <code>String</code>) as argument and return types,
 * and operations that search a file for a line with a specific property are provided by the
 * server.  The first principle ensures that scanning a database won't create a large number of remote objects that
 * must then be queried and garbage-collected (each requiring additional RPC).  The second
 * principle avoids paging an entire database file across the network in order to search for
 * an entry.
 *
 * <p>Making <code>FileBackedDictionary</code> XXX MISSING WORD XXX would violate the first of these properties
 * (it would require that {@link WordSense}, {@link Synset}, {@link POS}, etc. be supported as remote objects);
 * a generic remote file system interface would violate the second.
 *
 * <p>A third design principle is that sessions are stateless -- this simplifies the
 * implementation of the server.  A consequence of this
 * principle together with the restriction of return values to primitives is that pairs
 * of operations such as <code>getNextLinePointer</code>/<code>readLineAt</code> are required in order to step through
 * a file.  The implementor of <code>FileManagerInterface</code> can cache the file position before and
 * after <code>readLineAt</code> in order to eliminate the redundant IO activity that a naive implementation
 * of these methods would necessitate.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public interface FileManagerInterface extends Remote {
  /** Search for the line whose first word <i>is</i> <var>index</var> (that is, that begins with
   * <var>index</var> followed by a space or tab).  <var>filename</var> must name a file
   * whose lines are sorted by index word.
   * @return The file offset of the start of the matching line, or <code>-1</code> if no such line
   *         exists.
   */
  public int getIndexedLinePointer(String filename, String index) throws IOException, RemoteException;

  /** Read the line that begins at file offset <var>offset</var> in the file named by <var>filename</var>. */
  public String readLineAt(String filename, int offset) throws IOException, RemoteException;

  /** Search for the line following the line that begins at <var>offset</var>.
   * @return The file offset of the start of the line, or <code>-1</code> if <var>offset</var>
   *         is the last line in the file.
   */
  public int getNextLinePointer(String filename, int offset) throws IOException, RemoteException;

  /** Search for a line whose index word <i>contains</i> <var>substring</var>.
   * @return The file offset of the start of the matching line, or <code>-1</code> if
   *         no such line exists.
   */
  public int getMatchingLinePointer(String filename, int offset, String substring) throws IOException, RemoteException;

  /** Search for a line whose index word <i>begins with</i> <var>substring</var>.
   * @return The file offset of the start of the matching line, or <code>-1</code> if
   *         no such line exists.
   */
  public int getMatchingBeginningLinePointer(String filename, int offset, String substring) throws IOException, RemoteException;

  /** Treat file contents like an array of lines and return the zero-based,
   * inclusive line corresponding to <var>linenum</var> 
   */
  public String readLineNumber(String filename, int linenum) throws
  IOException;
}