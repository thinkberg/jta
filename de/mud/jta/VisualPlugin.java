/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Righs Reserved.
 *
 * The software is licensed under the terms and conditions in the
 * license agreement included in the software distribution package.
 *
 * You should have received a copy of the license along with this
 * software; see the file license.txt. If not, navigate to the 
 * URL http://javatelnet.org/ and view the "License Agreement".
 *
 */
package de.mud.jta;

import javax.swing.*;

/**
 * To show data on-screen a plugin may have a visible component. That component
 * may either be a single awt component or a container with severel elements.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public interface VisualPlugin {
  /**
   * Get the visible components from the plugin.
   * @return a component that represents the plugin
   */
  public JComponent getPluginVisual();

  /**
   * Get the menu entry for this component.
   * @return a menu that can be used to change the plugin state
   */
  public JMenu getPluginMenu();
}
