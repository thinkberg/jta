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
import de.mud.jta.PluginBus;
import de.mud.jta.VisualPlugin;
import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.SocketListener;

import java.awt.Component;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Menu;
import java.awt.Label;
import java.awt.Color;

import java.util.Hashtable;

/**
 * A simple plugin showing the current status of the application whether
 * it is online or not.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Status extends Plugin implements VisualPlugin {

  private final static int debug = 1;

  private Label status;
  private Label host;
  private Panel sPanel;

  private String address, port;

  private Hashtable ports = new Hashtable();

  public Status(PluginBus bus) {
    super(bus);

    // fill port hashtable
    ports.put("22", "ssh");
    ports.put("23", "telnet");
    ports.put("25", "smtp");
   
    sPanel = new Panel(new BorderLayout());

    host = new Label("Not connected.", Label.LEFT);

    bus.registerPluginListener(new SocketListener() {
      public void connect(String addr, int p) {
        address = addr;
	if(address == null || address.length() == 0)
	  address = "<unknwon host>";
	if(ports.get(""+p) != null)
	  port = (String)ports.get(""+p);
	else
	  port = ""+p;
        host.setText("Trying "+address+" "+port+" ...");
      }
      public void disconnect() {
        host.setText("Not connected.");
      }
    });

    sPanel.add("Center", host);

    status = new Label("offline", Label.CENTER);

    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        status.setText("online");
	status.setBackground(Color.green);
	host.setText("Connected to "+address+" "+port);
      }
      public void offline() {
        status.setText("offline");
	status.setBackground(Color.red);
        host.setText("Not connected.");
      }
    });

    sPanel.add("East", status);

  }

  public Component getPluginVisual() {
    return sPanel;
  }

  public Menu getPluginMenu() {
    return null;
  }
}
