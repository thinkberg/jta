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
package de.mud.jta.event;

import de.mud.jta.Plugin;
import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;

import java.awt.event.FocusEvent;

/**
 * Notify all listeners that a component has got the input focus.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class FocusStatus implements PluginMessage {
  protected Plugin plugin;
  protected FocusEvent event;

  /** Create a new online status message with the specified value. */
  public FocusStatus(Plugin plugin, FocusEvent event) {
    this.plugin = plugin;
    this.event = event;
  }

  /**
   * Notify the listers about the focus status of the sending component.
   * @param pl the list of plugin message listeners
   * @return null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof FocusStatusListener)
      switch(event.getID()) {
        case FocusEvent.FOCUS_GAINED:
          ((FocusStatusListener)pl).pluginGainedFocus(plugin); break;
	case FocusEvent.FOCUS_LOST:
          ((FocusStatusListener)pl).pluginLostFocus(plugin);
      }
    return null;
  }
}
