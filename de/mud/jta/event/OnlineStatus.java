/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
 *
 * Please visit http://javatelnet.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
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
