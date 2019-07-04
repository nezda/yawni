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

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

// TODO
// call this EditorKitHelper / StyledEditorKitHelper
class ActionHelper {
  private static final Map<String, Action> ACTIONS;
  private static final String SELECT_ALL_CUT = "SELECT_ALL_CUT";
  private static final String CLEAR = "CLEAR";
  static {
    final DefaultEditorKit dek = new DefaultEditorKit();
    ACTIONS = new HashMap<>();
    final Action[] actionsArray = dek.getActions();
    for (final Action a : actionsArray) {
      //System.err.println("a: "+a+" name: "+a.getValue(Action.NAME));
      ACTIONS.put((String) a.getValue(Action.NAME), a);
    }
    assert ACTIONS.containsKey(DefaultEditorKit.selectAllAction);
    assert ACTIONS.containsKey(DefaultEditorKit.cutAction);
    // goal: composite action: selectAllAction + cutAction
    ACTIONS.put(SELECT_ALL_CUT, compose(ACTIONS.get(DefaultEditorKit.selectAllAction), ACTIONS.get(DefaultEditorKit.cutAction)));
    ACTIONS.put(CLEAR, new TextAction("clear") {
      public void actionPerformed(ActionEvent e) {
        final JTextComponent target = getTextComponent(e);
        if (target != null) {
          target.setText("");
        }
      }
    });
  }

  private static Action compose(final Action... actions) {
    assert actions.length >= 2;
    String name = "";
    int ai = 0;
    for (final Action action : actions) {
      final String aname = (String) action.getValue(Action.NAME);
      assert aname != null && aname.length() > 0;
      name += aname;
      if (ai != actions.length - 1) {
        name += "-";
      }
    }
    return new AbstractAction(name) {
      public void actionPerformed(final ActionEvent e) {
        for (final Action action : actions) {
          action.actionPerformed(e);
        }
      }
    };
  }

  static Action selectAllCut() {
    return ACTIONS.get(SELECT_ALL_CUT);
  }
  static Action clear() {
    return ACTIONS.get(CLEAR);
  }
}