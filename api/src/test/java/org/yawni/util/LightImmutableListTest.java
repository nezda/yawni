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
package org.yawni.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LightImmutableListTest {
  @Test
  public void nothington() {
    System.out.println("nothington()");
    Integer[] elements = new Integer[]{};
    List<Integer> expResult = Collections.<Integer>emptyList();
    LightImmutableList<Integer> result = LightImmutableList.of();
    assertTrue(result instanceof LightImmutableList.Nothington);
    listTest(expResult, result);
    listTest(expResult, LightImmutableList.copyOf(expResult.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    assertTrue(resultFromArray instanceof LightImmutableList.Nothington);
    listTest(result, resultFromArray);
  }
  @Test
  public void singleton() {
    System.out.println("singleton()");
    Integer[] elements = new Integer[]{ 0 };
    Integer e0 = elements[0];
    List<Integer> expResult = Collections.singletonList(e0);
    LightImmutableList<Integer> result = LightImmutableList.of(e0);
    assertTrue(result instanceof LightImmutableList.Singleton);
    listTest(expResult, result);
    listTest(expResult, LightImmutableList.copyOf(expResult.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    assertTrue(resultFromArray instanceof LightImmutableList.Singleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void doubleton() {
    System.out.println("doubleton()");
    Integer[] elements = new Integer[]{ 0, 1 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    List<Integer> expResult1 = Arrays.asList(e0, e1);
    List<Integer> expResult2 = new ArrayList<Integer>(Arrays.asList(e0, e1));
    List<Integer> expResult3 = new LinkedList<Integer>(Arrays.asList(e0, e1));
    LightImmutableList<Integer> result = LightImmutableList.of(e0, e1);
    assertTrue(result instanceof LightImmutableList.Singleton); // implementation detail
    assertTrue(result instanceof LightImmutableList.Doubleton);
    listTest(expResult1, result);
    listTest(expResult2, result);
    listTest(expResult3, result);
    listTest(expResult1, LightImmutableList.copyOf(expResult1.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    assertTrue(resultFromArray instanceof LightImmutableList.Doubleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void tripleton() {
    System.out.println("tripleton()");
    Integer[] elements = new Integer[]{ 0, 1, 2 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    List<Integer> expResult = Arrays.asList(e0, e1, e2);
    LightImmutableList<Integer> result = LightImmutableList.of(e0, e1, e2);
    assertTrue(result instanceof LightImmutableList.Tripleton);
    listTest(expResult, result);
    listTest(expResult, LightImmutableList.copyOf(expResult.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    assertTrue(resultFromArray instanceof LightImmutableList.Tripleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void quadrupleton() {
    System.out.println("quadrupleton()");
    Integer[] elements = new Integer[]{ 0, 1, 2, 3 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    Integer e3 = elements[3];
    List<Integer> expResult = Arrays.asList(e0, e1, e2, e3);
    LightImmutableList<Integer> result = LightImmutableList.of(e0, e1, e2, e3);
    assertTrue(result instanceof LightImmutableList.Quadrupleton);
    listTest(expResult, result);
    listTest(expResult, LightImmutableList.copyOf(expResult.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    assertTrue(resultFromArray instanceof LightImmutableList.Quadrupleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void quintupleton() {
    System.out.println("quintupleton()");
    Integer[] elements = new Integer[]{ 0, 1, 2, 3, 4 };
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    Integer e3 = elements[3];
    Integer e4 = elements[4];
    List<Integer> expResult = Arrays.asList(e0, e1, e2, e3, e4);
    LightImmutableList<Integer> result = LightImmutableList.of(e0, e1, e2, e3, e4);
    assertTrue(result instanceof LightImmutableList.Quintupleton);
    listTest(expResult, result);
    listTest(expResult, LightImmutableList.copyOf(expResult.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    assertTrue(resultFromArray instanceof LightImmutableList.Quintupleton);
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
    LightImmutableList<Integer> result = LightImmutableList.of(e0, e1, e2, e3, e4, e5);
    //assertTrue(result instanceof LightImmutableList.Restleton);
    listTest(expResult, result);
    listTest(expResult, LightImmutableList.copyOf(expResult.iterator()));
    LightImmutableList<Integer> resultFromArray = LightImmutableList.of(elements);
    //assertTrue(resultFromArray instanceof LightImmutableList.Restleton);
    listTest(result, resultFromArray);
  }
  @Test
  public void iterable() {
    System.out.println("iterable()");
    Integer[] elements = new Integer[]{ 0, 1, 2, 3, 4};
    Integer e0 = elements[0];
    Integer e1 = elements[1];
    Integer e2 = elements[2];
    Integer e3 = elements[3];
    Integer e4 = elements[4];
    List<Integer> expResult = Collections.unmodifiableList(Arrays.asList(e0, e1, e2, e3, e4));
    LightImmutableList<Integer> result = LightImmutableList.copyOf(expResult);
    // compile test for covariant subList
    LightImmutableList<Integer> immutableSubList = result.subList(0, 1);
    assertTrue(result instanceof LightImmutableList.Quintupleton);
    listTest(expResult, result);
  }
  @Test
  public void commonOverloads() {
    System.out.println("commonOverloads()");
    List<List<Integer>> elements = new ArrayList<List<Integer>>();
    elements.add(Arrays.asList(0));
    elements.add(Arrays.asList(1));
    elements.add(Arrays.asList(2));
    elements.add(Arrays.asList(3));
    elements.add(Arrays.asList(4));
    List<Integer> e0 = elements.get(0);
    List<Integer> e1 = elements.get(1);
    List<Integer> e2 = elements.get(2);
    List<Integer> e3 = elements.get(3);
    List<Integer> e4 = elements.get(4);

    final List<List<Integer>> expResult = Collections.unmodifiableList(elements);
    @SuppressWarnings("unchecked") // Arrays.asList complains when making list of parameterized types
    final List<List<Integer>> altExpResult = Arrays.asList(e0, e1, e2, e3, e4);
    // LightImmutableList provides overloads for up to 5 arguments, and only for 6+ will
    // if fall back to pure varargs
    final LightImmutableList<List<Integer>> result = LightImmutableList.of(e0, e1, e2, e3, e4);
    // compile test for covariant subList
    //LightImmutableList<Integer> immutableSubList = result.subList(0, 1);
    assertTrue(result instanceof LightImmutableList.Quintupleton);
    listTest(expResult, result);
  }
  private <E> void listTest(List<E> expResult, List<E> result) {
    listTest(expResult, result, true);
  }
  private <E> void listTest(List<E> expResult, List<E> result, boolean recurse) {
    assertEquals(expResult, result);
    assertEquals(expResult.size(), result.size());
    assertEquals(
      "expResult class: "+expResult.getClass().getSimpleName()+
      " result class: "+result.getClass().getSimpleName(),
      expResult.toString(), result.toString());
    assertEquals(expResult.hashCode(), result.hashCode());
    assertTrue(Arrays.equals(expResult.toArray(), result.toArray()));
    @SuppressWarnings("unchecked")
    final E[] ref = (E[])new Object[0];
    assertTrue(Arrays.equals(expResult.toArray(ref), result.toArray(ref)));
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
        //System.out.println("result class: "+result.getClass().getSimpleName()+
        //  " result.size(): "+result.size()+" subList["+i+", "+expResult.size()+")");
        //System.out.println("expResult: "+expResult.subList(i, expResult.size()));
        //System.out.println("result: "+result.subList(i, expResult.size()));
        listTest(expResult.subList(i, expResult.size()), result.subList(i, expResult.size()), false);
      }
    }
  }
  @Test
  public void testNewArray() {
    Integer[] reference = new Integer[0];
    int length = 0;
    Integer[] expResult = reference;
    Integer[] result = LightImmutableList.newArray(reference, length);
    assertTrue(Arrays.equals(expResult, result));
  }
}