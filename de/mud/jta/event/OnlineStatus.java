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

import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;

/**
 * Notify all listeners that we on or offline.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class OnlineStatus implements PluginMessage {
  protected boolean online;

  /** Create a new online status message with the specified value. */
  public OnlineStatus(boolean online) {
    this.online = online;
  }

  /**
   * Notify the listers about the online status.
   * @param pl the list of plugin message listeners
   * @return the window size or null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof OnlineStatusListener)
      if(online)
        ((OnlineStatusListener)pl).online();
      else
        ((OnlineStatusListener)pl).offline();
    return null;
  }
}
