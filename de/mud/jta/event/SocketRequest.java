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
