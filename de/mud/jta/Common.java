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

import de.mud.jta.PluginLoader;
import de.mud.jta.VisualPlugin;
import de.mud.jta.Plugin;
import de.mud.jta.event.ConfigurationRequest;
import de.mud.jta.event.SocketRequest;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import java.awt.Component;
import java.awt.Menu;

/**
 * The common part of the <B>The Java<SUP>tm</SUP> Telnet Application</B>
 * is handled here. Mainly this includes the loading of the plugins and
 * the screen setup of the visual plugins.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Common extends PluginLoader {

  private Hashtable plugins, components, menus;

  public Common(Properties config) {
    System.out.println("** The Java(tm) Telnet Application");
    System.out.println("** Copyright (c) 1996-1999 Matthias L. Jugel, "
                      +"Marcus Meißner");

    plugins = new Hashtable();
    components = new Hashtable();
    menus = new Hashtable();

    Vector names = split(config.getProperty("plugins"));
    Enumeration e = names.elements();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      System.out.println("jta: loading plugin '"+name+"' ...");
      Plugin plugin = addPlugin(name);
      if(plugin instanceof VisualPlugin) {
        Component c = ((VisualPlugin)plugin).getPluginVisual();
	if(c != null) components.put(name, c);
	Menu menu = ((VisualPlugin)plugin).getPluginMenu();
	if(menu != null) menus.put(name, menu);
      }
    }

    broadcast(new ConfigurationRequest(config));
  }

  public Hashtable getComponents() {
    return components;
  }

  public Hashtable getMenus() {
    return menus;
  }

  /**
   * Split up comma separated lists of strings. This is quite strict, no
   * whitespace characters are allowed.
   * @param s the string to be split up
   * @return an array of strings
   */
  private static Vector split(String s) {
    Vector v = new Vector();
    int old = -1, idx = s.indexOf(',');
    while(idx >= 0) {
      v.addElement(s.substring(old + 1, idx));
      old = idx;
      idx = s.indexOf(',', old + 1);
    } 
    v.addElement(s.substring(old + 1));
    return v;
  }
}
