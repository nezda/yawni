/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.util.cache;

import java.io.Serializable;

/**
 * Interface for hash function objects; used by {@link BloomFilter BloomFilter&lt;E&gt;}.
 */
public interface Hasher<E> extends Serializable {
  /**
   * Returns a hash code for {@code e}, specialized for instances of {@code E},
   * otherwise delegating to {@link Object#hashCode()}.
   */
  int hashCode(Object e);
}