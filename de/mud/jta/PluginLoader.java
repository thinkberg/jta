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

import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

import java.lang.reflect.Constructor;

/**
 * The plugin loader tries to load the plugin by name and returns a
 * corresponding plugin object. It takes care of connecting filter
 * plugins
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class PluginLoader implements PluginBus {
  /** holds the current version id */
  public final static String ID = "$Id$";

  private final static int debug = 0;

  /** the path to standard plugins */
  private final static String PATH = "de.mud.jta.plugin.";

  /** holds all the filters */
  private Vector filter = new Vector();

  /**
   * Add a new plugin to the system and register the plugin load as its
   * communication bus. If the plugin is a filter plugin and if it is
   * not the first filter the last added filter will be set as its filter
   * source.
   * @param name the string name of the plugin
   * @return the newly created plugin or null in case of an error
   */
  public Plugin addPlugin(String name) {
    Plugin plugin = null;

    // load the plugin by name and instantiate it
    try {
      Class c = Class.forName(PATH+name);
      Constructor cc = c.getConstructor(new Class[] { PluginBus.class });
      plugin = (Plugin)cc.newInstance(new Object[] { this });
    } catch(Exception e) {
      System.err.println("plugin loader: cannot load "+name);
      e.printStackTrace();
      return null;
    }

    // configure the filter plugins
    if(plugin instanceof FilterPlugin) {
      if(filter.size() > 0)
        ((FilterPlugin)plugin)
          .setFilterSource((FilterPlugin)filter.lastElement());
      filter.addElement(plugin);
    }

    return plugin;
  }

  /** holds the plugin listener we serve */
  private Vector listener = new Vector();

  /**
   * Register a new plugin listener.
   */
  public void registerPluginListener(PluginListener l) {
    listener.addElement(l);
  }

  /**
   * Implementation of the plugin bus. Broadcast a message to all
   * listeners we know of. The message takes care that the right
   * methods are called in the  listeners.
   * @param message the plugin message to be sent
   * @return the answer to the sent message
   */
  public Object broadcast(PluginMessage message) {
    if(debug>0) System.err.println("broadcast("+message+")");
    if(message == null || listener == null)
      return null;
    Enumeration e = listener.elements();
    Object res = null;
    while(res == null && e.hasMoreElements())
      res = message.firePluginMessage((PluginListener)e.nextElement());
    return res;
  }
}

