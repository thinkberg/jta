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
import de.mud.jta.event.LocalEchoRequest;
import de.mud.jta.event.SocketListener;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * The shell plugin is the backend component for terminal emulation using
 * a shell. It provides the i/o streams of the shell as data source.
 * <P>
 * Thomas Kriegelstein wrote a hack that converted newlines to \r\n which
 * was used as an example for improving this plugin.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Shell extends Plugin implements FilterPlugin {

  private final static int debug = 1;

  protected String shellCommand;
  protected InputStream in, err;
  protected OutputStream out;
  protected Process p;

  public Shell(final PluginBus bus, final String id) {
    super(bus, id);

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig cfg) {
        String tmp;
	if((tmp = cfg.getProperty("Shell", id, "command")) != null)
	  shellCommand = tmp;
      }
    });
  
    bus.registerPluginListener(new SocketListener() {
      // we do actually ignore these parameters
      public void connect(String host, int port) {
	if(p == null) execute(host);
      }
      public void disconnect() {
	if(p != null) {
	  p.destroy();
	  p = null; in = null; out = null;
	}
	else execute(null);
      }
    });
  }

  private void execute(String command) {
    shellCommand = (command != null) ? command : shellCommand;
    if(shellCommand == null) return;
    Runtime rt = Runtime.getRuntime();
    try {
      p = rt.exec(shellCommand);
      in = p.getInputStream();
      out = p.getOutputStream();
      err = p.getErrorStream();
    } catch(Exception e) {
      Shell.this.error("error: "+e);
      e.printStackTrace();
    }
    bus.broadcast(new OnlineStatus(true));
  }

  public void setFilterSource(FilterPlugin plugin) {
    // we do not have a source other than our socket
  }

  private byte[] transpose(byte[] buf) {
    byte[] nbuf;
    int nbufptr = 0;
    nbuf = new byte[buf.length * 2];
    for(int i = 0; i < buf.length; i++)
      switch(buf[i]) {
        case 10: // \n
	  nbuf[nbufptr++] = 13;
	  nbuf[nbufptr++] = 10;
	  break;
	default:
	  nbuf[nbufptr++] = buf[i];
    }
    byte[] xbuf = new byte[nbufptr];
    System.arraycopy(nbuf, 0, xbuf, 0, nbufptr);
    return xbuf;
  }

  private byte buffer[];
  private int pos;

  public int read(byte[] b) throws IOException {
    // empty the buffer before reading more data
    if(buffer != null) {
      int amount = (buffer.length - pos) <= b.length ? 
                      buffer.length - pos : b.length;
      System.arraycopy(buffer, pos, b, 0, amount);
      if(pos + amount < buffer.length) {
        pos += amount;
      } else 
        buffer = null;
      return amount;
    }

    // now we are sure the buffer is empty and read on 
    int n = (err.available() > 0) ? err.read(b) : in.read(b);
    if(n > 0) {
      byte[] tmp = new byte[n];
      System.arraycopy(b, 0, tmp, 0, n);
      buffer = transpose(tmp);
      if(buffer != null && buffer.length > 0) {
        int amount = buffer.length <= b.length ? buffer.length : b.length;
        System.arraycopy(buffer, 0, b, 0, amount);
        pos = n = amount;
        if(amount == buffer.length) {
          buffer = null;
          pos = 0;
        }
      } else
        return 0;
    }
    return n;
  }


  public void write(byte[] b) throws IOException {
    out.write(b);
    out.flush();
  }
}
