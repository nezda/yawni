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
package org.yawni.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Helper class for creating correct custom {@link Enum} aliases and corresponding
 * static {@code valueOf(T)} methods.  Multiple objects (typically {@link String}s
 * or {@link Integer}s) can map to particular {@code Enum}s.
 * Created to work around limitations of enums.  Note that an enum can't have a(nother) static
 * {@code valueOf(String)} method, so another method name (e.g., 'forAlias') or
 * argument type (e.g., {@code Integer}) must be used.
 *
 * <p>Typical usage:<pre>{@code
 * enum MyEnum {
 *   CONSTANT_1(1),
 *   CONSTANT_2(2);
 *
 *   private MyEnum(final int id) {
 *     staticThis.ALIASES.registerAlias(this, id, name(), name().toLowerCase());
 *   }
 *
 *   public static MyEnum valueOf(final Integer id) {
 *     return staticThis.ALIASES.valueOf(id);
 *   }
 *
 *   // trick to allow referencing static EnumAliases instance from containing Enum constructor
 *   private static class staticThis {
 *     static EnumAliases<MyEnum> ALIASES = EnumAliases.make(MyEnum.class);
 *   }
 * }
 * }</pre>
 * @author nezda
 */
public class EnumAliases<E extends Enum<E>> {
  /**
   * Factory method to leverage type-inferencing to save you a few keystrokes.
   * @param clazz
   * @return new {@code EnumAliases} instance
   */
  public static <F extends Enum<F>> EnumAliases<F> make(final Class<F> clazz) {
    return new EnumAliases<>(clazz);
  }

  private final String enumName;
  private final Map<Object, E> mapping = Maps.newLinkedHashMap();

  /**
   * @param clazz enum {@link Class} literal; currently only used in exception messages, etc.
   */
  protected EnumAliases(final Class<E> clazz) {
    Preconditions.checkNotNull(clazz);
    this.enumName = clazz.getSimpleName();
  }

  /**
   * @param alias; throws {@link IllegalArgumentException} if given {@code alias} has not been registered
   * @return the enum corresponding to {@code alias}
   * @throws IllegalArgumentException
   */
  public E valueOf(final Object alias) {
    return valueOf(alias, true);
  }

  /**
   * @param alias
   * @param throwIfNull if true, throw {@link IllegalArgumentException} if given {@code alias} has not been registered,
   *        else return {@code null} for this case
   * @return the enum corresponding to {@code alias}
   * @throws IllegalArgumentException
   */
  public E valueOf(final Object alias, final boolean throwIfNull) {
    Preconditions.checkState(!mapping.isEmpty(), "You forgot to register aliases in your enum constructor.");
    final E toReturn = mapping.get(alias);
    Preconditions.checkArgument(!throwIfNull || toReturn != null, "Unknown %s alias %s", enumName, alias);
    return toReturn;
  }

  /**
   * @param e an enum instance
   * @param aliases zero or more aliases for {@code e}; {@code null} is not a supported alias
   * @throws IllegalStateException if alias refers to more than 1 enum value
   * @throws IllegalArgumentException if {@code aliases} includes {@code null}
   */
  public final void registerAlias(E e, Object... aliases) {
    Preconditions.checkNotNull(e);
    for (final Object alias : aliases) {
      Preconditions.checkArgument(alias != null, "null alias not supported");
      final E prev = mapping.put(alias, e);
      Preconditions.checkState(prev == null || e == prev,
        "Collision! prev: %s curr: %s alias: %s", prev, e, alias);
    }
  }

  //@Beta
  @Override
  public final String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("[EnumAliases for ").append(enumName).append(": [");
    Joiner.on(", ").withKeyValueSeparator("=>").appendTo(buffer, mapping);
    buffer.append("]]");
    return mapping.toString();
  }
}