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

import de.mud.jta.PluginListener;

import java.net.UnknownHostException;
import java.io.IOException;

/**
 * The socket listener should be implemented by plugins that want to know
 * when the whole systems connects or disconnects.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public interface SocketListener extends PluginListener {
  /** Called if a connection should be established. */
  public void connect(String host, int port)
    throws UnknownHostException, IOException;
  /** Called if the connection should be stopped. */
  public void disconnect() 
    throws IOException;
}
