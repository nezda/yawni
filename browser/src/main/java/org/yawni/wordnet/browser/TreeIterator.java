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
package org.yawni.wordnet.browser;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import com.google.common.collect.AbstractIterator;

/**
 * General programming technique called <a href="http://en.wikipedia.org/wiki/Continuation-passing_style">
 * continuation passing style</a> (CPS) which is a general technique to convert recursion into iteration.
 * Ceremoniously snipped from
 * <a href="http://lingpipe-blog.com/2009/01/27/quiz-answer-continuation-passing-style-for-converting-visitors-to-iterators/">
 * the great Bob Carpenter</a>'s blog.
 */
public class TreeIterator<T extends Object & Comparable<? super T>> implements Iterator<T> {
  //FIXME stack is a Vector which adds synchronization overhead
  // - push(T) == add(T)
  // - pop(T) == remove(size() - 1)
  private final Stack<Tree<T>> mStack = new Stack<Tree<T>>();

  TreeIterator(Tree<T> tree) {
    stackLeftDaughters(tree);
  }

  // factory of Iterables consisting of TreeIterator's of the given Tree
  static <T extends Object & Comparable<? super T>>
  Iterable<T> of(final Tree<T> tree) {
    return new Iterable<T>() {
      public Iterator<T> iterator() {
        return new TreeIterator<T>(tree);
      }
    };
  }

  public boolean hasNext() {
    return ! mStack.isEmpty();
  }

  public T next() {
    if (mStack.isEmpty()) {
      throw new NoSuchElementException();
    }
    Tree<T> t = mStack.pop();
    stackLeftDaughters(t.mRight);
    return t.mVal;
  }

  //LN junk
  public T nextInline() {
    if (mStack.isEmpty()) {
      throw new NoSuchElementException();
    }
    Tree<T> toReturn = mStack.pop();
//    stackLeftDaughters(t.mRight);
    Tree<T> t = toReturn.mRight;
    while (t != null) {
      mStack.push(t);
      t = t.mLeft;
    }
    return toReturn.mVal;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  private void stackLeftDaughters(Tree<T> t) {
    while (t != null) {
      mStack.push(t);
      t = t.mLeft;
    }
  }

  private void stackLeftDaughtersRecursive(Tree<T> t) {
    if (t == null) {
      return;
    }
    mStack.push(t);
    stackLeftDaughters(t.mLeft);
  }

  public static void main(String[] args) {
    args = new String[]{ "3", "7", "8", "1", "4", "5", "2", "5", "6" };
    final Integer val = Integer.valueOf(args[0]);
    final Tree<Integer> t = Tree.of(val);
    for (int i = 1; i < args.length; ++i) {
      t.add(Integer.valueOf(args[i]));
    }
    System.out.println("Tree prettyToString  = \n" + t.prettyToString());
    System.out.println("Tree toString  = " + t);
    System.out.println("Tree.preOrder  = " + t.preOrder());
    System.out.println("Tree.postOrder = " + t.postOrder());
    System.out.println("Tree.inOrder   = " + t.inOrder());
    System.out.print("TreeIterator   = ");
    for (final Integer i : TreeIterator.of(t)) {
      System.out.print(i+" ");
    }
    System.out.println();
  }
} // end class TreeIterator


class TreeIterator2<T extends Object & Comparable<? super T>> extends AbstractIterator<T> {
  private Tree<T> tree;
  TreeIterator2(final Tree<T> tree) {
    this.tree = tree;
  }
  @Override
  protected T computeNext() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
} // end class TreeIterator2

// more often this would be called a Node
class Tree<T extends Object & Comparable<? super T>> {
  final T mVal;
  Tree<T> mLeft;
  Tree<T> mRight;
  private Tree(T val) {
    mVal = val;
  }
  static <T extends Object & Comparable<? super T>>
    Tree<T> of(T val) {
    return new Tree<T>(val);
  }
  public void add(T val) {
    final int cmp = val.compareTo(mVal);
    if (cmp < 0) {
      if (mLeft == null) {
        mLeft = Tree.of(val);
      } else {
        mLeft.add(val);
      }
    } else if (cmp > 0) {
      if (mRight == null) {
        mRight = Tree.of(val);
      } else {
        mRight.add(val);
      }
    }
  }
  // aka Depth First Search (DFS)
  public String preOrder() {
    return
      mVal + " " +
      ((mLeft == null) ? "" : mLeft.preOrder()) +
      ((mRight == null) ? "" : mRight.preOrder());
  }
  public String inOrder() {
    return
      ((mLeft == null) ? "" : mLeft.inOrder()) +
      mVal + " " +
      ((mRight == null) ? "" : mRight.inOrder());
  }
  public String postOrder() {
    return
      ((mLeft == null) ? "" : mLeft.postOrder()) +
      ((mRight == null) ? "" : mRight.postOrder()) +
      mVal + " ";
  }
  // if output starting on a blank line, 'renders' the
  // tree as if rotated left 90 degrees (root in the left-most column,
  // 2nd level in the 2nd column, etc.)  This rendition will
  // be vertically off-center for some trees.
  public String prettyToString() {
    try {
      return prettyPrint(new StringBuilder(), "").toString();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  // recursive method
  public Appendable prettyPrint(Appendable output, String indent) throws IOException {
    if (mRight != null) {
      mRight.prettyPrint(output, indent + "  ");
    }
    //output.append(indent + this + "\n");
    output.append(indent + this.mVal + "\n");
    if (mLeft != null) {
      mLeft.prettyPrint(output, indent + "  ");
    }
    return output;
  }
  @Override
  public String toString() {
    return "(" + mVal + " " + mLeft + " " + mRight + ")";
  }
} // end class Tree