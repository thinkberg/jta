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

package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.FilterPlugin;
import de.mud.jta.PluginBus;
import de.mud.jta.event.SocketListener;
import de.mud.jta.event.OnlineStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * The socket plugin acts as the data source for networked operations.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Socket extends Plugin implements FilterPlugin, SocketListener {

  private final static int debug = 0;
 
  protected java.net.Socket socket;
  protected InputStream in;
  protected OutputStream out;

  /**
   * Create a new socket plugin.
   */
  public Socket(PluginBus pbus) {
    super(pbus);
    final PluginBus bus = pbus;

    // register socket listener
    bus.registerPluginListener(this);
  }

  /** Connect to the host and port passed. */
  public void connect(String host, int port) throws IOException {
    if(debug>0) System.err.println("Socket: connect("+host+","+port+")");
    try {
      socket = new java.net.Socket(host, port);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      bus.broadcast(new OnlineStatus(true));
    } catch(Exception e) {
      System.err.println("Socket: "+e);
      disconnect();
    }
  }  

  /** Disconnect the socket and close the connection. */
  public void disconnect() throws IOException {
    if(debug>0) System.err.println("Socket: disconnect()");
    bus.broadcast(new OnlineStatus(false));
    socket.close();
  }

  public void setFilterSource(FilterPlugin plugin) {
    // we do not have a source other than our socket
  }

  public int read(byte[] b) throws IOException {
    int n = in.read(b);
    if(n < 0) disconnect();
    return n;
  }

  public void write(byte[] b) throws IOException {
    try {
      out.write(b);
    } catch(IOException e) {
      disconnect();
    }
  }
}
