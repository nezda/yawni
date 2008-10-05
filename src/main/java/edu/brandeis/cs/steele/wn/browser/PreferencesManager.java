package edu.brandeis.cs.steele.wn.browser;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.prefs.*;

/**
 * Save window positions and all other persistent user preferences.
 * Doesn't use Properties (files), uses Preferences - no files or maps to manage.
 * TODO rename this class PreferencesManager
 * TODO remove static methods -- go through getInstance()
 * TODO check if importPreferences() clobbers saved preferences
 * TODO put WNHOME/WNSEARCHDIR per-OS defaults (same as those from .jnlp file) into preferences
 *      and reflect them into system properties
 * @author http://www.oreilly.com/catalog/swinghks/
 */
class PreferencesManager implements AWTEventListener {
  // import/export preferences
  // good article
  // http://blogs.sun.com/CoreJavaTechTips/entry/the_preferences_api

  private static Preferences prefs = Preferences.userNodeForPackage(PreferencesManager.class);
  private static PreferencesManager saver;

  static {
    // on class load, register self as saver
    //XXX Toolkit.getDefaultToolkit().addAWTEventListener(
    //XXX    PreferencesManager.getInstance(), AWTEvent.WINDOW_EVENT_MASK);

    //XXX for debugging
    //try {
    //  //prefs.clear();
    //  //prefs.removeNode();
    //  prefs = Preferences.userNodeForPackage(PreferencesManager.class);
    //} catch(BackingStoreException bse) {
    //  bse.printStackTrace();
    //}
    //TODO need way to do this in GUI since it clobbers user actions
    //loadDefaults();
  };

  static synchronized void loadDefaults() {
    // TODO
    // see if the defaults have been loaded,
    //   if not, load them
    // * Don't clobber user preferences
    // Parallel/isomorphic tree for user preferences and defaults
    // - like the package structure of src/java/ and test/
    // - user
    // - default
    // Add a revision number or something to determine the defaults tree version
    // Preferences.userNodeForPackage(Class<?> c)
    //
    // TODO how to represent search history ?
    // Note: when encoding values, there is a maximum size:
    // Preferences.MAX_VALUE_LENGTH characters

    final InputStream is = PreferencesManager.class.getResourceAsStream("defaults.xml");
    try {
      Preferences.importPreferences(is);
      is.close();
    } catch(InvalidPreferencesFormatException ipfe) {
      throw new RuntimeException(ipfe);
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  enum LookAndFeel {
    System {
      public void set() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
          java.lang.System.err.println("Error setting LAF "+this+" " + e);
        }
      }
    },
    CrossPlatform {
      public void set() {
        try {
          UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch(Exception e) {
          java.lang.System.err.println("Error setting LAF "+this+" " + e);
        }
      }
    },
    GTK {
      public void set() {
        try {
          UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
          //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        } catch(Exception e) {
          java.lang.System.err.println("Error setting LAF "+this+" " + e);
        }
      }
    }
    ;

    public abstract void set();
  } // end enum LookAndFeel

  static void setLookAndFeel() {
    //TODO loadDefaults();
    //LookAndFeel.GTK.set();
    LookAndFeel.System.set();
    // even OS X LAF has bugs
    //LookAndFeel.CrossPlatform.set();

    //System.getProperty("os.name").toLowerCase().startsWith("mac os x");

    //for(final UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
    //  System.err.println("lafInfo: "+lafInfo);
    //}

    // ideal behavior:
    // if not Linux (OS X or Windows)
    //   use System
    // else
    //   use CrossPlatform
  }

  public static PreferencesManager getInstance() {
    if (saver == null) {
      saver = new PreferencesManager();
    }
    return saver;
  }

  private PreferencesManager() {
    //final InputStream is = PreferencesManager.class.getResourceAsStream("defaults.xml");
    //try {
    //  final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    //  String line;
    //  while((line = reader.readLine()) != null) {
    //    System.err.println(line);
    //  }
    //  reader.close();
    //} catch(IOException ioe) {
    //  throw new RuntimeException(ioe);
    //}
    //
  }

  /** {@inheritDoc} */
  public void eventDispatched(final AWTEvent evt) {
    //System.err.println("event: " + evt);
    if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
      final ComponentEvent cev = (ComponentEvent)evt;
      if (cev.getComponent() instanceof JFrame) {
        //System.err.println("closing event: " + evt);
        final JFrame frame = (JFrame)cev.getComponent();
        final String name = frame.getName();
        if (name.startsWith("edu.brandeis.cs.steele.wn.browser") == false) {
          return;
        }
        //XXX saveSettings(frame);
        //covered by WindowClosing listener
      }
    }
    if (evt.getID() == WindowEvent.WINDOW_OPENED) {
      final ComponentEvent cev = (ComponentEvent)evt;
      if (cev.getComponent() instanceof JFrame) {
        //System.err.println("closing event: " + evt);
        final JFrame frame = (JFrame)cev.getComponent();
        final String name = frame.getName();
        if (name.startsWith("edu.brandeis.cs.steele.wn.browser") == false) {
          return;
        }
        loadSettings(frame);
      }
    }
  }

  // TODO use background thread to update current window position on
  // move or use an ugly shutdown hook to make sure saves work

  static void loadSettings(final JFrame frame) {
    // "Window settings"
    final String name = frame.getName();
    //System.err.println("load name: " + name);
    final int x = prefs.getInt(name + ".x", -1);
    final int y = prefs.getInt(name + ".y", -1);
    final int w = prefs.getInt(name + ".width", 640);
    final int h = prefs.getInt(name + ".height", 480);
    //FIXME interpret width / height 0 as preferred
    frame.setSize(new Dimension(w, h));
    frame.validate();

    if(x >= 0 && y >= 0) {
      frame.setLocation(x, y);
    } else {
      frame.setLocationRelativeTo(null);
    }
  }

  static void saveSettings(final JFrame frame) {
    final String name = frame.getName();
    System.err.println("save name: " + name);
    prefs.putInt(name + ".x", frame.getX());
    prefs.putInt(name + ".y", frame.getY());
    prefs.putInt(name + ".width", frame.getWidth());
    prefs.putInt(name + ".height", frame.getHeight());
  }
}
