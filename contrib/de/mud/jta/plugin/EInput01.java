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
import de.mud.jta.VisualPlugin;
import de.mud.jta.event.ConfigurationListener;

import java.awt.Component;
import java.awt.Menu;
import java.awt.Panel;
import java.awt.Button;
import java.awt.TextArea;
import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

/**
 * An example plugin that creates a text area and sends the text entered
 * there to the remote host. The example explains how to create a filter
 * plugin (for sending only) and a visual plugin.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */

public class EInput01 extends Plugin
  implements FilterPlugin, VisualPlugin {

  protected TextArea input;
  protected Button send;
  protected Panel panel;

  public EInput01(PluginBus bus, final String id) {
    super(bus, id);

    // create text field and send button
    input = new TextArea(10, 30);
    send = new Button("Send Text");

    // add action listener to send the text
    send.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        try {
          // calls the write from FilterPlugin interface
          write(input.getText().getBytes());
        } catch(Exception e) {
          System.err.println("EInput01: error sending text: "+e);
          e.printStackTrace();
        }
      }
    });

    // create a panel and put text field and button into it.
    panel = new Panel(new BorderLayout());
    panel.add("Center", input);
    panel.add("South", send);
  }

  
  /** the source where we get data from */
  protected FilterPlugin source = null;

  public void setFilterSource(FilterPlugin plugin) {
    source = plugin;
  }

  /**
   * Read data from the filter plugin source and return the amount read.
   * We do not really do anything here
   * @param b the array where to read the bytes in
   * @return the amount of bytes actually read
   */
  public int read(byte[] b) throws IOException {
    return source.read(b);
  }

  /**
   * Write data to the filter plugin source. This method is used by the
   * visual components of the plugin to send data.
   */
  public void write(byte[] b) throws IOException {
    source.write(b);
  }

  /**
   * This method returns the visual part of the component to be displayed
   * by the applet or application at the specified location in the config
   * file.
   * @return a visual Component
   */
  public Component getPluginVisual() {
    return panel;
  }

  /**
   * If you want to have a menu configure it and return it here.
   * @return the plugin menu
   */
  public Menu getPluginMenu() {
    return null;
  }
}
