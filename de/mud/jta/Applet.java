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

import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.SocketRequest;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import java.io.IOException;
import java.net.URL;

import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Label;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * <B>The Java Telnet Applet</B><P>
 * This is the implementation of whole set of applications. It's modular
 * structure allows to configure the software to act either as a sophisticated
 * terminal emulation and/or, adding the network backend, as telnet 
 * implementation. Additional modules provide features like scripting or an
 * improved graphical user interface.<P>
 * This software is written entirely in Java<SUP>tm</SUP>.<P>
 * This is the <I>Applet</I> implementation for the software. It initializes
 * the system and adds all needed components, such as the telnet backend and
 * the terminal front end. 
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Applet extends java.applet.Applet {

  /** holds the defaults */
  private Properties options = new Properties();

  /** hold the common part of the jta */
  private Common pluginLoader;

  /** hold the host and port for our connection */
  private String host, port;

  /**
   * Read all parameters from the applet configuration and
   * do initializations for the plugins and the applet.
   */
  public void init() {
    if(pluginLoader == null) {
      try {
        options.load(getClass()
	  .getResourceAsStream("/de/mud/jta/defaults.opt"));
      } catch(Exception e) {
        System.err.println("jta: cannot load defaults from classpath");
        System.err.println("-- This may be due to a security restriction in\n"
	                  +"-- Netscape. Trying to load alternative ...");
	try {
          URL url = new URL(getCodeBase() + "defaults.opt");
	  System.err.println("-- loading "+url);
          options.load(url.openStream());
	} catch(Exception e1) {
	  System.err.println("jta: alternative defaults file not found");
        }
      }

      String value;

      // try to load the local configuration and merge it with the defaults
      if((value = getParameter("config")) != null) {
        Properties appletParams = new Properties();
	try {
	  appletParams.load((new URL(getCodeBase() + value)).openStream());
	  Enumeration ape = appletParams.keys();
	  while(ape.hasMoreElements()) {
	    String key = (String)ape.nextElement();
	    options.put(key, appletParams.getProperty(key));
	  }
	} catch(Exception e) {
	  System.err.println("jta: could not load config file: "+e);
	}
      }

      if((value = getParameter("plugins")) != null) {
        System.err.println("jta: 'plugins' is deprecated, use config!");
        options.put("plugins", value);
      }
      if((value = getParameter("port")) != null) {
        System.err.println("jta: 'Socket.port' is deprecated, use config!");
        options.put("Socket.port", value);
      }
      if((value = getParameter("layout")) != null) {
        System.err.println("jta: 'layout' is deprecated, use config!");
        options.put("layout", value);
      }

      // let the terminal resize to the max possible
      options.put("Terminal.resize", "font");

      // configure the application and load all plugins
      pluginLoader = new Common(options);

      // set the host to our code base, no other hosts are allowed anyway
      host = getCodeBase().getHost();
      port = options.getProperty("Socket.port");
      if(port == null) port = "23";

      setLayout(new BorderLayout());

      Hashtable componentList = pluginLoader.getComponents();
      Enumeration names = componentList.keys();
      while(names.hasMoreElements()) {
        String name = (String)names.nextElement();
        Component c = (Component)componentList.get(name);
	if(getParameter("layout."+name) != null)
	  System.err.println("jta: 'layout.*' is deprecated use config!");
        if((value = getParameter("layout."+name)) != null ||
	   (value = options.getProperty("layout."+name)) != null) {
          add(value, c);
        } else {
          System.err.println("jta: no layout property set for '"+name+"'");
          System.err.println("jta: ignoring '"+name+"'");
	}
      }
    }
  }

  /**
   * Start the applet. Connect to the remote host.
   */
  public void start() {
    getAppletContext().showStatus("Trying "+host+" "+port+" ...");
    pluginLoader.broadcast(new SocketRequest(host, Integer.parseInt(port)));
  }

  /**
   * Stop the applet and disconnect.
   */
  public void stop() {
    pluginLoader.broadcast(new SocketRequest());
  }
}
