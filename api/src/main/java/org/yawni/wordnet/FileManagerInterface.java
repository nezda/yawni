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

import java.io.IOException;
import java.util.Comparator;
import java.util.regex.Matcher;

/**
 * {@code FileManagerInterface} defines the interface between {@link WordNet} and the data file system.
 * {@code WordNet} invokes methods from this interface to retrieve lines of text from the
 * WordNet data files.
 *
 * <p> Methods in this interface take filenames as arguments.  The filename is the name of
 * a WordNet file, and is relative to the database directory (e.g., {@code data.noun}, not
 * {@code dict/data.noun}).
 *
 * <p> Methods in this interface operate on and return pointers, which are indices into the
 * file named by filename.
 *
 * <p> {@code FileManagerInterface} was originally designed to work efficiently across a network.  To this end, it obeys
 * two design principles:  1) it uses only primitive types (including {@code String}) as argument and return types,
 * and 2) operations that search a file for a line with a specific property are provided by the
 * server.  The first principle ensures that scanning a database won't create a large number of remote objects that
 * must then be queried and garbage-collected (each requiring an additional <abbr title="Remote Procedure Call">RPC</abbr>).  The second
 * principle avoids paging an entire database file across the network in order to search for
 * an entry.
 *
 * <p> Making {@code WordNet} {@linkplain java.io.Serializable} would violate the first of these properties
 * (it would require that {@link Word}, {@link Synset}, {@link POS}, {@link WordSense}, etc. also be supported as remote objects);
 * a generic remote file system interface would violate the second.
 *
 * <p> A third design principle is that sessions are <em>stateless</em> -- this simplifies the
 * implementation of the server.  A consequence of this
 * principle together with the restriction of return values to primitives is that pairs
 * of operations such as {@code getNextLinePointer}/{@code readLineAt} are required in order to step through
 * a file.  The implementor of {@code FileManagerInterface} can cache the file position before and
 * after {@code readLineAt} in order to eliminate the redundant I/O activity that a naïve implementation
 * of these methods would necessitate.
 */
interface FileManagerInterface {
  /**
   * Binary searches for line whose first word <em>is</em> {@code target} (that
   * is, that begins with {@code target} followed by a space or dash) in
   * file implied by {@code fileName}.  Assumes this file is sorted by its
   * first textual column of <em>lowercased</em> words.  This condition can be verified
   * with UNIX <tt>sort</tt> with the command <tt>sort -k1,1 -c</tt>.
   * If the array contains multiple elements with the specified value, there is no
   * guarantee which one will be found.
   * @param target string sought
   * @param fileName filename to search; fileNameWnRelative = {@code true}
   * @return The file offset of the start of the matching line if one exists.
   * Otherwise, {@code (-(insertion point) - 1)}.
   * The insertion point is defined as the point at which the target would be
   * inserted into the file: the index of the first element greater than the
   * target, or the row count, if all elements in the file are less than the
   * specified target. Note that this guarantees that the return value will be
   * {@code >= 0} if and only if the target is found.  This is analogous to
   * {@link java.util.Arrays#binarySearch java.util.Arrays.binarySearch()}.
   * @throws IOException
   * @see #comparator()
   */
  public int getIndexedLinePointer(final CharSequence target, final String fileName) throws IOException;

  /**
   * @param target string sought
   * @param start file offset to start at
   * @param fileName
   * @param filenNameWnRelative if {@code true}, {@code fileName} is relative to <tt>WNSEARCHDIR</tt>, else
   * {@code fileName} is absolute or classpath relative.
   * @throws IOException
   */
  public int getIndexedLinePointer(final CharSequence target, int start, final String fileName, final boolean filenNameWnRelative) throws IOException;

  /**
   * Efficient query method for sorted input which may have duplicates that returns ALL matches.
   * @param target prefix word sought
   * @param fileName
   * @throws IOException
   * @see #getIndexedLinePointer(java.lang.CharSequence, java.lang.String)
   */
  public Iterable<CharSequence> getMatchingLines(final CharSequence target, final String fileName) throws IOException;

  /**
   * Read the line that begins at file offset {@code offset} in the file named by {@code fileName}.
   * @throws IOException
   */
  public String readLineAt(final int offset, final String fileName) throws IOException;

  /**
   * Search for the line following the line that begins at {@code offset}.
   * @return The file offset of the start of the line, or {@code -1} if {@code offset}
   *         is the last line in the file.
   * @throws IOException
   */
  public int getNextLinePointer(final int offset, final String fileName) throws IOException;

  /**
   * Search for a line whose index word <em>contains</em> {@code pattern} (case insensitive).
   * @return The file offset of the start of the matching line, or {@code -1} if
   *         no such line exists.
   * @throws IOException
   */
  public int getMatchingLinePointer(final int offset, final Matcher pattern, final String fileName) throws IOException;

  /**
   * Search for a line whose index word <em>begins with</em> {@code prefix} (case insensitive).
   * @return The file offset of the start of the matching line, or {@code -1} if
   *         no such line exists.
   * @throws IOException
   */
  public int getPrefixMatchLinePointer(final int offset, final CharSequence prefix, final String fileName) throws IOException;

  /**
   * Treat file contents like an array of lines and return the zero-based,
   * inclusive line corresponding to {@code linenum; not currently implemented efficiently
   * @throws IOException
   */
  public String readLineNumber(final int linenum, final String fileName) throws IOException;

  /**
   * The {@link Comparator Comparator<CharSequence>} that defines the sort order of the WordNet data files.
   */
  public Comparator<CharSequence> comparator();
}