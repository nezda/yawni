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

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class EnumAliasesTest {
  private enum MyEnum {
    CONSTANT_1(1),
    CONSTANT_2(2);

    private MyEnum(final int id) {
      staticThis.ALIASES.registerAlias(this, id, name(), name().toLowerCase());
    }

    public static MyEnum valueOf(final Integer id) {
      return staticThis.ALIASES.valueOf(id);
    }

    public static MyEnum noThrowValueOf(final Integer id) {
      return staticThis.ALIASES.valueOf(id, false);
    }

    public static MyEnum forAlias(final String id) {
      return staticThis.ALIASES.valueOf(id);
    }

    public static MyEnum noThrowForAlias(final String id) {
      return staticThis.ALIASES.valueOf(id, false);
    }

    // trick to allow referencing static EnumAliases instance from containing Enum constructor
    private static class staticThis {
      static EnumAliases<MyEnum> ALIASES = EnumAliases.make(MyEnum.class);
    }
  }

  final String alternatingCaseConstant1Label = "cOnStAnT_1";

  @Test
  public void testValueOf() {
    assertThat(MyEnum.valueOf("CONSTANT_1")).isSameAs(MyEnum.CONSTANT_1);
    assertThat(MyEnum.valueOf(1)).isSameAs(MyEnum.CONSTANT_1);
    assertThat(MyEnum.valueOf("CONSTANT_2")).isSameAs(MyEnum.CONSTANT_2);
    assertThat(MyEnum.valueOf(2)).isSameAs(MyEnum.CONSTANT_2);

    assertThat(alternatingCaseConstant1Label.equalsIgnoreCase(MyEnum.CONSTANT_1.name()));
    assertThat(MyEnum.noThrowValueOf(-1)).isNull();
    assertThat(MyEnum.noThrowForAlias(alternatingCaseConstant1Label)).isNull();
  }

  @Test(expected=IllegalArgumentException.class)
  public void testValueOfThrows() {
    assertThat(MyEnum.valueOf(-1)).isNull();
  }

  final String someAlias = "someAlias";
  final String someOtherAlias = "someOtherAlias";

  @Test(expected=IllegalStateException.class)
  public void testDuplicateAlias() {
    // can't do method-local enum, external aliases is close enough
    final EnumAliases<MyEnum> aliases = EnumAliases.make(MyEnum.class);
    aliases.registerAlias(MyEnum.CONSTANT_1, someAlias);
    aliases.registerAlias(MyEnum.CONSTANT_2, someAlias);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testNullAlias() {
    // can't do method-local enum, external aliases is close enough
    final EnumAliases<MyEnum> aliases = EnumAliases.make(MyEnum.class);
    aliases.registerAlias(MyEnum.CONSTANT_1, someAlias);
    final String nullString = null;
    aliases.registerAlias(MyEnum.CONSTANT_2, nullString);
  }

  @Test
  public void testOkDuplicateAlias() {
    // can't do method-local enum, external aliases is close enough
    final EnumAliases<MyEnum> aliases = EnumAliases.make(MyEnum.class);
    // same mapping twice (weird, but ok)
    aliases.registerAlias(MyEnum.CONSTANT_1, someAlias);
    aliases.registerAlias(MyEnum.CONSTANT_1, someAlias);

    aliases.registerAlias(MyEnum.CONSTANT_2, someOtherAlias);

    assertThat(aliases.valueOf(someAlias)).isSameAs(MyEnum.CONSTANT_1);
    assertThat(aliases.valueOf(someOtherAlias)).isSameAs(MyEnum.CONSTANT_2);
  }
}
