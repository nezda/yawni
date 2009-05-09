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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package org.yawni.wn;

import java.io.IOException;
import java.util.Comparator;

/**
 * {@code FileManagerInterface} defines the interface between the {@code FileBackedDictionary} and the data file system.
 * {@code FileBackedDictionary} invokes methods from this interface to retrieve lines of text from the
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
 * <p> Making {@code FileBackedDictionary} {@linkplain Serializable} would violate the first of these properties
 * (it would require that {@link Word}, {@link Synset}, {@link POS}, {@link WordSense}, etc. also be supported as remote objects);
 * a generic remote file system interface would violate the second.
 *
 * <p> A third design principle is that sessions are <em>stateless</em> -- this simplifies the
 * implementation of the server.  A consequence of this
 * principle together with the restriction of return values to primitives is that pairs
 * of operations such as {@code getNextLinePointer}/{@code readLineAt} are required in order to step through
 * a file.  The implementor of {@code FileManagerInterface} can cache the file position before and
 * after {@code readLineAt} in order to eliminate the redundant IO activity that a naïve implementation
 * of these methods would necessitate.
 */
public interface FileManagerInterface {
  /**
   * Binary searches for line whose first word <em>is</em> {@code target} (that
   * is, that begins with {@code target} followed by a space or dash) in
   * file implied by {@code filename}.  Assumes this file is sorted by its
   * first textual column of lowercased words.  This condtion can be verified
   * with UNIX <tt>sort</tt> with the command <tt>sort -k1,1 -c</tt>
   * @return The file offset of the start of the matching line if one exists.
   * Otherwise, return  {@code (-(insertion point) - 1)}.
   * The insertion point is defined as the point at which the target would be
   * inserted into the file: the index of the first element greater than the
   * target, or list.size(), if all elements in the file are less than the
   * specified target. Note that this guarantees that the return value will be
   * {@code >= 0} if and only if the target is found.  This is analagous to
   * {@link java.util.Arrays#binarySearch java.util.Arrays.binarySearch()}.
   * @throws IOException
   * @see #comparator()
   */
  public int getIndexedLinePointer(final CharSequence target, final String filename) throws IOException;

  /**
   * @param filenameWnRelative if {@code true}, {@code filename} is relative to <tt>WNSEARCHDIR</tt>, else
   * {@code filename} is absolute or classpath relative.
   * @param start
   * @throws IOException
   */
  public int getIndexedLinePointer(final CharSequence target, int start, final String filename, final boolean filenameWnRelative) throws IOException;

  /**
   * Read the line that begins at file offset {@code offset} in the file named by {@code filename}.
   * @throws IOException
   */
  public String readLineAt(final int offset, final String filename) throws IOException;

  /**
   * Search for the line following the line that begins at {@code offset}.
   * @return The file offset of the start of the line, or {@code -1} if {@code offset}
   *         is the last line in the file.
   * @throws IOException
   */
  public int getNextLinePointer(final int offset, final String filename) throws IOException;

  /**
   * Search for a line whose index word <em>contains</em> {@code substring}.
   * @return The file offset of the start of the matching line, or {@code -1} if
   *         no such line exists.
   * @throws IOException
   */
  public int getMatchingLinePointer(final int offset, final CharSequence substring, final String filename) throws IOException;

  /**
   * Search for a line whose index word <em>begins with</em> {@code prefix}.
   * @return The file offset of the start of the matching line, or {@code -1} if
   *         no such line exists.
   * @throws IOException
   */
  public int getPrefixMatchLinePointer(final int offset, final CharSequence prefix, final String filename) throws IOException;

  /**
   * Treat file contents like an array of lines and return the zero-based,
   * inclusive line corresponding to {@code linenum}
   * @throws IOException
   */
  public String readLineNumber(final int linenum, final String filename) throws IOException;

  /**
   * The {@link Comparator<CharSequence>} that defines the sort order of the WordNet data files.
   */
  public Comparator<CharSequence> comparator();
}