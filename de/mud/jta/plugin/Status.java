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

import java.awt.Component;
import java.awt.Menu;
import java.awt.Label;
import java.awt.Color;

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

  public Status(PluginBus bus) {
    super(bus);
    status = new Label("offline", Label.RIGHT);
    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        status.setText("online");
	status.setBackground(Color.green);
      }
      public void offline() {
        status.setText("offline");
	status.setBackground(Color.red);
      }
    });
  }

  public Component getPluginVisual() {
    return status;
  }

  public Menu getPluginMenu() {
    return null;
  }
}
