package org.yawni.wn.browser;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;

class ActionHelper {
  private static final Map<String, Action> ACTIONS;
  private static final String SELECT_ALL_CUT = "SELECT_ALL_CUT";
  static {
    final DefaultEditorKit dek = new DefaultEditorKit();
    ACTIONS = new HashMap<String, Action>();
    final Action[] actionsArray = dek.getActions();
    for (int i = 0; i < actionsArray.length; i++) {
      final Action a = actionsArray[i];
      //System.err.println("a: "+a+" name: "+a.getValue(Action.NAME));
      ACTIONS.put((String) a.getValue(Action.NAME), a);
    }
    assert ACTIONS.containsKey(DefaultEditorKit.selectAllAction);
    assert ACTIONS.containsKey(DefaultEditorKit.cutAction);
    // goal: composite action: selectAllAction + cutAction
    ACTIONS.put(SELECT_ALL_CUT, compose(ACTIONS.get(DefaultEditorKit.selectAllAction), ACTIONS.get(DefaultEditorKit.cutAction)));
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
      private static final long serialVersionUID = 1L;

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
}