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
import de.mud.jta.PluginConfig;
import de.mud.jta.event.SocketListener;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import java.awt.Frame;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The socket plugin acts as the data source for networked operations.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Socket extends Plugin 
  implements FilterPlugin, SocketListener {

  private final static int debug = 0;
 
  protected java.net.Socket socket;
  protected InputStream in;
  protected OutputStream out;

  protected String relay = null;
  protected int relayPort = 31415;

  /**
   * Create a new socket plugin.
   */
  public Socket(final PluginBus bus, final String id) {
    super(bus, id);

    // register socket listener
    bus.registerPluginListener(this);

     bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig config) {
        if((relay = config.getProperty("Socket", id, "relay")) 
	   != null)
	  if(config.getProperty("Socket", id, "relayPort") != null) 
	    try {
	      relayPort = Integer.parseInt(
	         config.getProperty("Socket", id, "relayPort"));
	  } catch(NumberFormatException e) {
	    Socket.this.error("relayPort is not a number");
	  }
       }
    });
  }

  private String error = null;

  /** 
   * Connect to the host and port passed. If the multi relayd (mrelayd) is
   * used to allow connections to any host and the Socket.relay property
   * is configured this method will connect to the relay first, send
   * off the string "relay host port\n" and then the real connection will
   * be published to be online.
   */
  public void connect(String host, int port) throws IOException {
    if(host == null) return;
    if(debug>0) error("connect("+host+","+port+")");
    try {
      // check the relay settings, this is for the mrelayd only!
      if(relay == null)
        socket = new java.net.Socket(host, port);
      else
        socket = new java.net.Socket(relay, relayPort);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      // send the string to relay to the target host, port
      if(relay != null)
        write(("relay "+host+" "+port+"\n").getBytes());
    } catch(Exception e) {
      error = "Sorry, Could not connect: "+e+"\r\n\r\n"+
              "Your are either behind a firewall or the Java Telnet Applet\r\n"+
	      "has a broken configuration.\r\n\r\n"+
              "If unsure, please contact the administrator "+
              "of the web page.\r\n";
      error("can't connect: "+e);
    }
    bus.broadcast(new OnlineStatus(true));
  }  

  /** Disconnect the socket and close the connection. */
  public void disconnect() throws IOException {
    if(debug>0) error("disconnect()");
    bus.broadcast(new OnlineStatus(false));
    if(socket != null) {
      socket.close();
      in = null; out = null;
    }
  }

  public void setFilterSource(FilterPlugin plugin) {
    // we do not have a source other than our socket
  }

  public int read(byte[] b) throws IOException {
    // send error messages upward
    if(error != null && error.length() > 0) {
      int n = error.length() < b.length ? error.length() : b.length;
      System.arraycopy(error.getBytes(), 0, b, 0, n);
      error = error.substring(n);
      return n;
    }

    if(in == null) {
      disconnect();
      return -1;
    }

    int n = in.read(b);
    if(n < 0) disconnect();
    return n;
  }

  public void write(byte[] b) throws IOException {
    if(out == null) return;
    try {
      out.write(b);
    } catch(IOException e) {
      disconnect();
    }
  }
}
