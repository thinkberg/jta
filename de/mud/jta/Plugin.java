/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Righs Reserved.
 *
 * The software is licensed under the terms and conditions in the
 * license agreement included in the software distribution package.
 *
 * You should have received a copy of the license along with this
 * software; see the file license.txt. If not, navigate to the 
 * URL http://javatelnet.org/ and view the "License Agreement".
 *
 */
package de.mud.jta;


/**
 * Plugin base class for the Java Telnet Application. A plugin is a component
 * for the PluginBus and may occur several times. If we have more than one
 * plugin of the same type the protected value id contains the unique plugin
 * id as configured in the configuration.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Plugin {
  /** holds the plugin bus used for communication between plugins */
  protected PluginBus bus;
  /**
   * in case we have several plugins of the same type this contains their
   * unique id
   */
  protected String id;

  /**
   * Create a new plugin and set the plugin bus used by this plugin and
   * the unique id. The unique id may be null if there is only one plugin
   * used by the system.
   * @param bus the plugin bus
   * @param id the unique plugin id
   */
  public Plugin(PluginBus bus, String id) {
    this.bus = bus;
    this.id = id;
  }

  /**
   * Return identifier for this plugin.
   * @return id string
   */
  public String getId() {
    return id;
  }

  /**
   * Print an error message to stderr prepending the plugin name. This method
   * is public due to compatibility with Java 1.1
   * @param msg the error message
   */
  public void error(String msg) {
    String name = getClass().toString();
    name = name.substring(name.lastIndexOf('.') + 1);
    System.err.println(name + (id != null ? "(" + id + ")" : "") + ": " + msg);
  }
}
