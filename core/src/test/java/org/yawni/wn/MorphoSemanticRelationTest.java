package org.yawni.wn;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class MorphoSemanticRelationTest {
  @Test
  public void test() {
    System.err.println("values: "+MorphoSemanticRelation.getStringToRelMap());
    assertThat(MorphoSemanticRelation.AGENT).isSameAs(MorphoSemanticRelation.valueOf("AGENT"));
    assertThat(MorphoSemanticRelation.fromValue("AGENT")).isSameAs(MorphoSemanticRelation.valueOf("AGENT"));
    
    assertThat(MorphoSemanticRelation.fromValue("BY_MEANS_OF")).isSameAs(MorphoSemanticRelation.BY_MEANS_OF);
    assertThat(MorphoSemanticRelation.fromValue("by-means-of")).isSameAs(MorphoSemanticRelation.BY_MEANS_OF);
  }
}