package org.yawni.wn;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class TeleologicalRelationTest {
  @Test
  public void test() {
    System.err.println("values: "+TeleologicalRelation.getStringToRelMap());
    assertThat(TeleologicalRelation.AGENT).isSameAs(TeleologicalRelation.valueOf("AGENT"));
    assertThat(TeleologicalRelation.fromValue("AGENT")).isSameAs(TeleologicalRelation.valueOf("AGENT"));

    assertThat(TeleologicalRelation.fromValue("ACTION")).isSameAs(TeleologicalRelation.ACTION);
    assertThat(TeleologicalRelation.fromValue("action")).isSameAs(TeleologicalRelation.ACTION);
  }
}