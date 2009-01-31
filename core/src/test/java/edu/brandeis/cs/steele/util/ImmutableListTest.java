package edu.brandeis.cs.steele.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImmutableListTest {
  @Test
  public void nothington() {
    Integer[] elements = new Integer[]{};
    List<Integer> expResult = Collections.<Integer>emptyList();
    ImmutableList<Integer> result = ImmutableList.of();
    assertTrue(result instanceof ImmutableList.Nothington);
    listTest(expResult, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Nothington);
    listTest(result, resultFromArray);
  }
  @Test
  public void singleton() {
    Integer[] elements = new Integer[]{ 0 };
    Integer e0 = elements[0];
    List<Integer> expResult = Collections.singletonList(e0);
    ImmutableList<Integer> result = ImmutableList.of(e0);
    assertTrue(result instanceof ImmutableList.Singleton);
    listTest(expResult, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Singleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void doubleton() {
    Integer[] elements = new Integer[]{ 0, 1 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    List<Integer> expResult1 = Arrays.asList(e0, e1);
    List<Integer> expResult2 = new ArrayList<Integer>(Arrays.asList(e0, e1));
    List<Integer> expResult3 = new LinkedList<Integer>(Arrays.asList(e0, e1));
    ImmutableList<Integer> result = ImmutableList.of(e0, e1);
    assertTrue(result instanceof ImmutableList.Singleton); // implementation detail
    assertTrue(result instanceof ImmutableList.Doubleton);
    listTest(expResult1, result);
    listTest(expResult2, result);
    listTest(expResult3, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Doubleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void tripleton() {
    Integer[] elements = new Integer[]{ 0, 1, 2 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    List<Integer> expResult = Arrays.asList(e0, e1, e2);
    ImmutableList<Integer> result = ImmutableList.of(e0, e1, e2);
    assertTrue(result instanceof ImmutableList.Tripleton);
    listTest(expResult, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Tripleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void quadrupleton() {
    Integer[] elements = new Integer[]{ 0, 1, 2, 3 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    Integer e3 = elements[3];
    List<Integer> expResult = Arrays.asList(e0, e1, e2, e3);
    ImmutableList<Integer> result = ImmutableList.of(e0, e1, e2, e3);
    assertTrue(result instanceof ImmutableList.Quadrupleton);
    listTest(expResult, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Quadrupleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void quintupleton() {
    Integer[] elements = new Integer[]{ 0, 1, 2, 3, 4 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    Integer e3 = elements[3];
    Integer e4 = elements[4];
    List<Integer> expResult = Arrays.asList(e0, e1, e2, e3, e4);
    ImmutableList<Integer> result = ImmutableList.of(e0, e1, e2, e3, e4);
    assertTrue(result instanceof ImmutableList.Quintupleton);
    listTest(expResult, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Quintupleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void restleton() {
    System.out.println("restleton()");
    Integer[] elements = new Integer[]{ 0, 1, 2, 3, 4, 5 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    Integer e3 = elements[3];
    Integer e4 = elements[4];
    Integer e5 = elements[5];
    List<Integer> expResult = Arrays.asList(e0, e1, e2, e3, e4, e5);
    ImmutableList<Integer> result = ImmutableList.of(e0, e1, e2, e3, e4, e5);
    assertTrue(result instanceof ImmutableList.Restleton);
    listTest(expResult, result);
    ImmutableList<Integer> resultFromArray = ImmutableList.of(elements);
    assertTrue(resultFromArray instanceof ImmutableList.Restleton);
    listTest(result, resultFromArray);
  }
  private <E> void listTest(List<E> expResult, List<E> result) {
    listTest(expResult, result, true);
  }
  private <E> void listTest(List<E> expResult, List<E> result, boolean recurse) {
//    System.out.println("expResult: "+expResult+" result: "+result);
    assertEquals(expResult, result);
    assertEquals(expResult.size(), result.size());
    assertEquals(
      "expResult class: "+expResult.getClass().getSimpleName()+
      " result class: "+result.getClass().getSimpleName(),
      expResult.toString(), result.toString());
    assertEquals(expResult.hashCode(), result.hashCode());
    assertTrue(expResult.containsAll(result));
    assertTrue(result.containsAll(expResult));
    for (E e : expResult) {
      assertTrue(result.contains(e));
    }
    // TODO test toArray
    // test subList
    if (recurse) {
      // {0, 0}, {0,1}, {0,2}, ..., {0, size()}
      for (int i=0; i <= expResult.size(); i++) {
        listTest(expResult.subList(0, i), result.subList(0, i), false);
      }
      // {0, size()}, {1, size()}, {2,size()}, ..., {size(), size()}
      for (int i = 0; i <= expResult.size(); i++) {
        System.out.println("result class: "+result.getClass().getSimpleName()+
          " result.size(): "+result.size()+" subList["+i+", "+expResult.size()+")");
        System.out.println("expResult: "+expResult.subList(i, expResult.size()));
        System.out.println("result: "+result.subList(i, expResult.size()));
        listTest(expResult.subList(i, expResult.size()), result.subList(i, expResult.size()), false);
      }
    }
  }
  @Test
  public void testNewArray() {
    Integer[] reference = new Integer[]{};
    int length = 0;
    Integer[] expResult = reference;
    Integer[] result = ImmutableList.newArray(reference, length);
    assertTrue(Arrays.equals(expResult, result));
  }
}