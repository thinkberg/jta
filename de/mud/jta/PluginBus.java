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
