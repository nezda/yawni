package edu.brandeis.cs.steele.wn.browser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Allow mouse drags from anywhere in a window to move the window.
 * @author http://www.oreilly.com/catalog/swinghks/
 */
class MoveMouseListener implements MouseListener, MouseMotionListener {
  private final JComponent target;
  private Point startDrag;
  private Point startLoc;

  public MoveMouseListener(final JComponent target) {
    this.target = target;
    this.startDrag = null;
    this.startLoc = null;
  }

  Point getScreenLocation(final MouseEvent evt) {
    final Point cursor = evt.getPoint();
    final Point target_location = this.target.getLocationOnScreen();
    return new Point(
        (int)(target_location.getX() + cursor.getX()),
        (int)(target_location.getY() + cursor.getY()));
  }

  public void mouseClicked(final MouseEvent evt) {}
  public void mouseEntered(final MouseEvent evt) {}
  public void mouseExited(final MouseEvent evt) {}
  public void mousePressed(final MouseEvent evt) {
    this.startDrag = this.getScreenLocation(evt);
    this.startLoc = MoveMouseListener.getFrame(this.target).getLocation();
  }
  public void mouseReleased(final MouseEvent evt) {}
  public void mouseDragged(final MouseEvent evt) {
    final Point current = this.getScreenLocation(evt);
    final Point offset = new Point(
        (int)current.getX() - (int)startDrag.getX(),
        (int)current.getY() - (int)startDrag.getY());
    final JFrame frame = MoveMouseListener.getFrame(target);
    final Point new_location = new Point(
        (int)(this.startLoc.getX() + offset.getX()),
        (int)(this.startLoc.getY() + offset.getY()));
    frame.setLocation(new_location);
  }
  public void mouseMoved(final MouseEvent evt) {}

  static JFrame getFrame(final Container target) {
    if(target instanceof JFrame) {
      return (JFrame)target;
    }
    return MoveMouseListener.getFrame(target.getParent());
  }
}
