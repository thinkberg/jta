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

  public final static String DEFAULT_PATH = "de.mud.jta.plugin";

  public Common(Properties config) {
    // configure the plugin path
    super(getPluginPath(config.getProperty("pluginPath")));

    System.out.println("** The Java(tm) Telnet Application");
    System.out.println("** Version 2.0 for Java 1.1.x and Java 2");
    System.out.println("** Copyright (c) 1996-2000 Matthias L. Jugel, "
                      +"Marcus Meissner");

    plugins = new Hashtable();
    components = new Hashtable();
    menus = new Hashtable();

    Vector names = split(config.getProperty("plugins"), ',');
    if(names == null) {
      System.err.println("jta: no plugins found! aborting ...");
      return;
    }

    Enumeration e = names.elements();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      String id = null; int idx;
      if((idx = name.indexOf("(")) > 1) {
        if(name.indexOf(")", idx) > idx)
          id = name.substring(idx + 1, name.indexOf(")", idx));
	else
	  System.err.println("jta: missing ')' for plugin '"+name+"'");
        name = name.substring(0, idx);
      }
      System.out.println("jta: loading plugin '"+name+"'"
                        +(id != null && id.length() > 0 ? 
			    ", ID: '"+id+"'" : ""));
      Plugin plugin = addPlugin(name, id);
      plugins.put(name, plugin);
      if(plugin instanceof VisualPlugin) {
        Component c = ((VisualPlugin)plugin).getPluginVisual();
	if(c != null) components.put(name+(id != null ? "("+id+")" : ""), c);
	Menu menu = ((VisualPlugin)plugin).getPluginMenu();
	if(menu != null) menus.put(name+(id != null ? "("+id+")" : ""), menu);
      }
    }

    broadcast(new ConfigurationRequest(new PluginConfig(config)));
  }

  public Hashtable getPlugins() {
    return plugins;
  }

  public Hashtable getComponents() {
    return components;
  }

  public Hashtable getMenus() {
    return menus;
  }


  /**
   * Convert the plugin path from a separated string list to a Vector.
   * @param path the string path
   * @return a vector containing the path
   */
  private static Vector getPluginPath(String path) {
    if(path == null)
      path = DEFAULT_PATH;

    // I am not sure that this is desirable, as the applet administrator
    // might use UNIX and thus ':' but the applet user might have a Windows
    // system and thus uses ';' and thus the whole thing will collapse.
    String separator = System.getProperty("path.separator");
    if(separator == null)
      separator = ":";

    return split(path, separator.charAt(0));
  }

  /**
   * Split up comma separated lists of strings. This is quite strict, no
   * whitespace characters are allowed.
   * @param s the string to be split up
   * @return an array of strings
   */
  private static Vector split(String s, char separator) {
    if(s == null) return null;
    Vector v = new Vector();
    int old = -1, idx = s.indexOf(separator);
    while(idx >= 0) {
      v.addElement(s.substring(old + 1, idx));
      old = idx;
      idx = s.indexOf(separator, old + 1);
    } 
    v.addElement(s.substring(old + 1));
    return v;
  }
}
