/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
 *
 * The software is licensed under the terms and conditions in the
 * license agreement included in the software distribution package.
 *
 * You should have received a copy of the license along with this
 * software; see the file license.txt. If not, navigate to the 
 * URL http://javatelnet.org/ and view the "License Agreement".
 *
 */

package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.FilterPlugin;
import de.mud.jta.PluginBus;
import de.mud.jta.PluginConfig;
import de.mud.jta.event.LocalEchoRequest;
import de.mud.jta.event.SocketListener;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatus;

// import java.io.InputStream;
// import java.io.OutputStream;
import java.io.IOException;

/**
 * The shell plugin is the backend component for terminal emulation using
 * a shell. It provides the i/o streams of the shell as data source.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner, Pete Zaitcev
 */
public class Shell extends Plugin implements FilterPlugin {

  protected String shellCommand;

  private HandlerPTY pty;

  public Shell(final PluginBus bus, final String id) {
    super(bus, id);

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig cfg) {
        String tmp;
	if((tmp = cfg.getProperty("Shell", id, "command")) != null) {
	  shellCommand = tmp;
	  // System.out.println("Shell: Setting config " + tmp); // P3
        } else {
	  // System.out.println("Shell: Not setting config"); // P3
	  shellCommand = "/bin/sh";
        }
      }
    });

    bus.registerPluginListener(new SocketListener() {
      // we do actually ignore these parameters
      public void connect(String host, int port) {
        // XXX Fix this together with window size changes
        // String ttype = (String)bus.broadcast(new TerminalTypeRequest());
        // String ttype = getTerminalType();
        // if(ttype == null) ttype = "dumb";

	// XXX Add try around here to catch missing DLL/.so.
	pty = new HandlerPTY();

        if(pty.start(shellCommand) == 0) {
	  bus.broadcast(new OnlineStatus(true));
        } else {
	  bus.broadcast(new OnlineStatus(false));
        }
      }
      public void disconnect() {
        bus.broadcast(new OnlineStatus(false));
        pty = null;
      }
    });
  }

  public void setFilterSource(FilterPlugin plugin) {
    // we do not have a source other than our socket
  }

  public FilterPlugin getFilterSource() {
    return null;
  }

  public int read(byte[] b) throws IOException {
    if(pty == null) return 0;
    int ret = pty.read(b);
    if(ret <= 0) {
      throw new IOException("EOF on PTY");
    }
    return ret;
  }

  public void write(byte[] b) throws IOException {
    if(pty != null) pty.write(b);
  }
}
