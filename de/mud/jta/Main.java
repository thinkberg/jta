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

import java.io.IOException;

import java.awt.Frame;
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
 * <B>The Java Telnet Application</B><P>
 * This is the implementation of whole set of applications. It's modular
 * structure allows to configure the software to act either as a sophisticated
 * terminal emulation and/or, adding the network backend, as telnet 
 * implementation. Additional modules provide features like scripting or an
 * improved graphical user interface.<P>
 * This software is written entirely in Java<SUP>tm</SUP>.<P>
 * This is the main program for the command line telnet. It initializes the
 * system and adds all needed components, such as the telnet backend and
 * the terminal front end. In contrast to applet functionality it parses
 * command line arguments used for configuring the software. Additionally
 * this application is not restricted in the sense of Java<SUP>tmp</SUP>
 * security.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Main extends Frame {

  public static void main(String args[]) {
    Properties options = new Properties();
    try {
      options.load(options.getClass()
                     .getResourceAsStream("/de/mud/jta/defaults.opt"));
    } catch(IOException e) {
      System.err.println("jta: cannot load defaults");
    }
    String error = parseOptions(options, args);
    if(error != null) {
      System.err.println(error);
      System.err.println("usage: de.mud.jta.Main [-addplugin plugin] "
                        +"[-term id] [host [port]]");
      System.exit(0);
    }

    final String host = options.getProperty("Socket.host");
    final String port = options.getProperty("Socket.port");

    final Frame frame = new Frame("jta: "+host+(port.equals("23")?"":" "+port));

    // configure the application and load all plugins
    final Common setup = new Common(options);

    setup.registerPluginListener(new OnlineStatusListener() {
      public void online() { 
        frame.setTitle("jta: "+host+(port.equals("23")?"":" "+port));
      }
      public void offline() {
        frame.setTitle("jta: offline");
      }
    });

    Hashtable componentList = setup.getComponents();
    Enumeration names = componentList.keys();
    while(names.hasMoreElements()) {
      String name = (String)names.nextElement();
      Component c = (Component)componentList.get(name);
      if(options.getProperty("layout."+name) == null) {
        System.err.println("jta: no layout property set for '"+name+"'");
	frame.add("South", c);
      } else
        frame.add(options.getProperty("layout."+name), c);
    }

    // add a menu bar
    MenuBar mb = new MenuBar();
    Menu file = new Menu("Host");
    file.setShortcut(new MenuShortcut(KeyEvent.VK_H, true));
    MenuItem tmp;
    file.add(tmp = new MenuItem("Connect"));
    tmp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        setup.broadcast(new SocketRequest(host, Integer.parseInt(port)));
      }
    });
    file.add(tmp = new MenuItem("Disconnect"));
    tmp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        setup.broadcast(new SocketRequest());
      }
    });
    file.add(new MenuItem("-"));
    file.add(tmp = new MenuItem("Exit"));
    tmp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        frame.dispose();
	System.exit(0);
      }
    });
    mb.add(file);

    Hashtable menuList = setup.getMenus();
    names = menuList.keys();
    while(names.hasMoreElements()) {
      String name = (String)names.nextElement();
      mb.add((Menu)menuList.get(name));
    }

    Menu help = new Menu("Help");
    help.add(tmp = new MenuItem("About"));
    tmp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        Dialog d = new Dialog(frame, "About JTA", true);
	d.add(new Label("Copyright (c) 1996-1999 "
	               +"Matthias L Jugel, Marcus Meiﬂner"));
	d.setResizable(false);
	d.pack();
	d.show();
      }
    });
    mb.setHelpMenu(help);

    frame.setMenuBar(mb);
    frame.pack();
    frame.show();

    setup.broadcast(new SocketRequest(host, Integer.parseInt(port)));
  }

  /**
   * Parse the command line argumens and override any standard options
   * with the new values if applicable.
   * <P><SMALL>
   * This method did not work with jdk 1.1.x as the setProperty()
   * method is not available. So it uses now the put() method from 
   * Hashtable instead.
   * </SMALL>
   * @param options the original options
   * @param args the command line parameters
   * @return a possible error message if problems occur
   */
  private static String parseOptions(Properties options, String args[]) {
    boolean host = false, port = false;
    for(int n = 0; n < args.length; n++) {
      if(args[n].equals("-addplugin"))
        if(!args[n+1].startsWith("-"))
	  options.put("plugins", args[++n]+","+options.get("plugins"));
        else
	  return "missing parameter for -addplugin";
      else if(args[n].equals("-term"))
        if(!args[n+1].startsWith("-"))
          options.put("Terminal.id", args[++n]);
        else 
	  return "missing parameter for -term";
      else if(!host) {
	options.put("Socket.host", args[n]); host = true;
      } else if(host && !port) {
	options.put("Socket.port", args[n]); port = true;
      } else
        return "unknown parameter '"+args[n]+"'";
    }
    return null;
  }
}
