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
import de.mud.jta.event.AppletRequest;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import java.io.IOException;
import java.net.URL;

import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Label;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;

import java.awt.datatransfer.Clipboard;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

  /** disconnect on leave, this is to force applets to break the connection */
  private boolean disconnect = true;
  /** close the window (if it exists) after the connection is lost */
  private boolean disconnectCloseWindow = true;

  private Plugin focussedPlugin;
  private Clipboard clipboard;
	private boolean Online=false;

  /**
   * Read all parameters from the applet configuration and
   * do initializations for the plugins and the applet.
   */
  public void init() {
		System.err.println("In init");
    if(pluginLoader == null) {
      try {
        options.load(getClass()
	  .getResourceAsStream("/de/mud/jta/default.conf"));
      } catch(Exception e) {
	try {
          URL url = new URL(getCodeBase() + "default.conf");
          options.load(url.openStream());
	} catch(Exception e1) {
	  System.err.println("jta: cannot load default.conf");
	  System.err.println("jta: try extracting it from the jar file");
	  System.err.println("jta: expected file here: "
	                    +getCodeBase()+ "default.conf");
        }
      }

      String value;

      // try to load the local configuration and merge it with the defaults
      if((value = getParameter("config")) != null) {
        Properties appletParams = new Properties();
	URL url = null;
	try {
	  url = new URL(value);
	} catch(Exception e) {
	  try {
	    url = new URL(getCodeBase() + value);
	  } catch(Exception ce) { 
	    System.err.println("jta: could not find config file: "+ce);
	  }
	} 

	if(url != null) {
	  try {
	    appletParams.load(url.openStream());
	    Enumeration ape = appletParams.keys();
	    while(ape.hasMoreElements()) {
	      String key = (String)ape.nextElement();
	      options.put(key, appletParams.getProperty(key));
	    }
	  } catch(Exception e) {
	    System.err.println("jta: could not load config file: "+e);
	  }
	}
      }

      // see if there are parameters in the html to override properties
      parameterOverride(options);

      // configure the application and load all plugins
      pluginLoader = new Common(options);

      // set the host to our code base, no other hosts are allowed anyway
      host = options.getProperty("Socket.host");
      if(host == null)
        host = getCodeBase().getHost();
      port = options.getProperty("Socket.port");
      if(port == null)
        port = "23";


      if(!(new Boolean(options.getProperty("Applet.disconnect"))
            .booleanValue()))
        disconnect = false;

      if(!(new Boolean(options.getProperty("Applet.disconnect.closeWindow"))
            .booleanValue()))
        disconnectCloseWindow = false;

  		final java.awt.Container appletFrame ;

      if((new Boolean(options.getProperty("Applet.detach"))).booleanValue()) {
        appletFrame = new Frame("jta: "+host+(port.equals("23")?"":" "+port));
     } else
        appletFrame = this;

      appletFrame.setLayout(new BorderLayout());

      Hashtable componentList = pluginLoader.getComponents();
      Enumeration names = componentList.keys();
      while(names.hasMoreElements()) {
        String name = (String)names.nextElement();
        Component c = (Component)componentList.get(name);
        if((value = options.getProperty("layout."+name)) != null) {
          appletFrame.add(value, c);
        } else {
          System.err.println("jta: no layout property set for '"+name+"'");
          System.err.println("jta: ignoring '"+name+"'");
	}
      }

      if(appletFrame != this) {
	final String startText = options.getProperty("Applet.detach.startText");
	final String stopText = options.getProperty("Applet.detach.stopText");
	final Button close =  new Button();
	  
	if((new Boolean(options.getProperty("Applet.detach.immediately"))
	     .booleanValue())) {
          ((Frame)appletFrame).pack();
	  ((Frame)appletFrame).show();
          pluginLoader.broadcast(new AppletRequest(this));
	  close.setLabel(startText != null ? stopText : "Disconnect");
	} else
	  close.setLabel(startText != null ? startText : "Connect");

	close.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent evt) {
	    if(((Frame)appletFrame).isVisible()) {
	      pluginLoader.broadcast(new SocketRequest());
	      ((Frame)appletFrame).setVisible(false);
	      close.setLabel(startText != null ? startText : "Connect");
	    } else {
	      ((Frame)appletFrame).pack();
              ((Frame)appletFrame).show();
	      Applet.this.start();
	      close.setLabel(stopText != null ? stopText : "Disconnect");
	    }
          }
	});

	 setLayout(new BorderLayout());
	 add("Center", close);

	// add a menu bar
        MenuBar mb = new MenuBar();
        Menu file = new Menu("File");
        file.setShortcut(new MenuShortcut(KeyEvent.VK_H, true));
        MenuItem tmp;
        file.add(tmp = new MenuItem("Connect"));
        tmp.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            pluginLoader.broadcast(new SocketRequest(host, 
	                                             Integer.parseInt(port)));
          }
        });
        file.add(tmp = new MenuItem("Disconnect"));
        tmp.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            pluginLoader.broadcast(new SocketRequest());
          }
        });
	file.add(new MenuItem("-"));
        file.add(tmp = new MenuItem("Exit"));
        tmp.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            ((Frame)appletFrame).setVisible(false);
	    pluginLoader.broadcast(new SocketRequest());
	    close.setLabel(startText != null ? startText : "Connect");
          }
        });
	mb.add(file);

	Menu edit = new Menu("Edit");
        edit.setShortcut(new MenuShortcut(KeyEvent.VK_H, true));
        edit.add(tmp = new MenuItem("Copy"));
        tmp.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            if(focussedPlugin instanceof VisualTransferPlugin)
	      ((VisualTransferPlugin)focussedPlugin).copy(clipboard);
          }
        });
        edit.add(tmp = new MenuItem("Paste"));
        tmp.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            if(focussedPlugin instanceof VisualTransferPlugin)
	      ((VisualTransferPlugin)focussedPlugin).paste(clipboard);
          }
        });
        mb.add(edit);

	Hashtable menuList = pluginLoader.getMenus();
        names = menuList.keys();
        while(names.hasMoreElements()) {
          String name = (String)names.nextElement();
          mb.add((Menu)menuList.get(name));
        }

	// only add the menubar if the property is true
        if((new Boolean(options.getProperty("Applet.detach.menuBar"))
           .booleanValue()))
          ((Frame)appletFrame).setMenuBar(mb);

	// add window closing event handler
        try {
          ((Frame)appletFrame).addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent evt) {
	      pluginLoader.broadcast(new SocketRequest());
              ((Frame)appletFrame).setVisible(false);
	      close.setLabel(startText != null ? startText : "Connect");
	    }
	  });
	} catch(Exception e) {
	  System.err.println("Applet: could not set up Window event listener");
	  System.err.println("Applet: you will not be able to close it");
	}

	pluginLoader.registerPluginListener(new OnlineStatusListener() {
	  public void online() {
            Online=true;
            if(((Frame)appletFrame).isVisible() == false)
      	      ((Frame)appletFrame).setVisible(true);
	  }
	  public void offline() {
	    if(disconnectCloseWindow)
	      ((Frame)appletFrame).setVisible(false);
            Online=false;
	    close.setLabel(startText != null ? startText : "Connect");
	  }
	});
      }

    }
  }

  /**
   * Start the applet. Connect to the remote host.
   */
  public void start() {
		if(Online == false) {
    	getAppletContext().showStatus("Trying "+host+" "+port+" ...");
    	pluginLoader.broadcast(new SocketRequest(host, Integer.parseInt(port)));
		} 
  }

  /**
   * Stop the applet and disconnect.
   */
  public void stop() {
    if(disconnect)
      pluginLoader.broadcast(new SocketRequest());
  }

  /**
   * Override any properties that are found in the configuration files
   * with possible values found as applet parameters.
   * @param options the loaded configuration file properties
   */
  private void parameterOverride(Properties options) {
    Enumeration e = options.keys();
    while(e.hasMoreElements()) {
      String key = (String)e.nextElement(), value = getParameter(key);
      if(value != null) {
	System.out.println("Applet: overriding value of "+key+" with "+value);
        // options.setProperty(key, value);
        options.put(key, value);
      }
    }
  }
}
