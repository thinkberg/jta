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
import de.mud.jta.event.LocalEchoRequest;
import de.mud.jta.event.SocketListener;
import de.mud.jta.event.OnlineStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * The shell plugin is the backend component for terminal emulation using
 * a shell. It provides the i/o streams of the shell as data source.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Shell extends Plugin implements FilterPlugin {

  private final static int debug = 1;

  protected InputStream in, err;
  protected OutputStream out;

  public Shell(final PluginBus bus) {
    super(bus);

    bus.registerPluginListener(new SocketListener() {
      public void connect(String host, int port) {
        Runtime rt = Runtime.getRuntime();
        try {
          Process p = rt.exec(host);
          in = p.getInputStream();
          out = p.getOutputStream();
          err = p.getErrorStream();
        } catch(Exception e) {
          System.err.println("Shell: "+e);
          e.printStackTrace();
        }
        bus.broadcast(new OnlineStatus(true));
      }
      public void disconnect() {
        // ignore
      }
    });
  }

  public void setFilterSource(FilterPlugin plugin) {
    // we do not have a source other than our socket
  }

  public int read(byte[] b) throws IOException {
    System.err.println("Shell: read()");
    if(err.available() > 0) return err.read(b);
    return in.read(b);
  }

  public void write(byte[] b) throws IOException {
    System.err.println("Shell: write()");
    out.write(b);
    out.flush();
  }
}
