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
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import java.util.Properties;

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
  public Socket(final PluginBus bus) {
    super(bus);

    // register socket listener
    bus.registerPluginListener(this);

     bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(Properties config) {
        if((relay = config.getProperty("Socket.relay")) != null)
	  if(config.getProperty("Socket.relayPort") != null) try {
	    relayPort=Integer.parseInt(config.getProperty("Socket.relayPort"));
	  } catch(NumberFormatException e) {
	    System.err.println("Socket: relayPort is not a number");
	  }
       }
    });
  }

  /** 
   * Connect to the host and port passed. If the multi relayd (mrelayd) is
   * used to allow connections to any host and the Socket.relay property
   * is configured this method will connect to the relay first, send
   * off the string "relay host port\n" and then the real connection will
   * be published to be online.
   */
  public void connect(String host, int port) throws IOException {
    if(host == null) return;
    if(debug>0) System.err.println("Socket: connect("+host+","+port+")");
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
      bus.broadcast(new OnlineStatus(true));
    } catch(Exception e) {
      final Frame frame = new Frame("Java Telnet Applet: Socket Error");
      Panel p = new Panel(new BorderLayout());
      TextArea msg = new TextArea(
        "Your are either behind a firewall or the\n"+
	"Java Telnet Applet has a broken configuration.\n\n"+
        "The error is:\n"+e+"\n\n"+
        "If unsure, please contact the administrator"+
        "of the web page.\n", 10, 40);
      msg.setEditable(false);
      p.add("Center", msg);
      Button close = new Button("Close Window");
      close.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
	  frame.dispose();
	}
      });
      p.add("South", close);
      frame.add("Center", p);
      frame.pack();
      frame.show();
      System.err.println("Socket: "+e);
      disconnect();
    }
  }  

  /** Disconnect the socket and close the connection. */
  public void disconnect() throws IOException {
    if(debug>0) System.err.println("Socket: disconnect()");
    bus.broadcast(new OnlineStatus(false));
    if(socket != null) socket.close();
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
