/*
 * This file is part of "The Java Telnet Application".
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * "The Java Telnet Application" is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package de.mud.jta.event;

import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;
import de.mud.jta.event.SocketListener;

/**
 * Notification of a socket request. Send this message if the system
 * should connect or disconnect.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class SocketRequest implements PluginMessage {
  String host;
  int port;

  /** Create a new disconnect message */
  public SocketRequest() {
    host = null;
  }

  /** Create a new connect message */
  public SocketRequest(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Tell all listeners that we would like to connect.
   * @param pl the list of plugin message listeners
   * @return the terminal type or null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof SocketListener) try {
      if(host != null) 
        ((SocketListener)pl).connect(host, port);
      else
        ((SocketListener)pl).disconnect();
    } catch(Exception e) { 
      e.printStackTrace();
    }
    return null;
  }
}
