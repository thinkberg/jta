/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
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

/**
 * A plugin bus is used for communication between plugins. The interface
 * describes the broadcast method that should broad cast the message
 * to all plugins known and return an answer message immediatly.<P>
 * The functionality is just simuliar to a bus, but depends on the
 * actual implementation of the bus.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public interface PluginBus {
  /** Broadcast a plugin message to all listeners. */
  public Object broadcast(PluginMessage message);
  /** Register a plugin listener with this bus object */
  public void registerPluginListener(PluginListener listener);
}
