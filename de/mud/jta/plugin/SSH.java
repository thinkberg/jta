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

import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.TerminalTypeRequest;
import de.mud.jta.event.WindowSizeRequest;
import de.mud.jta.event.LocalEchoRequest;

import de.mud.ssh.SshIO;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Label;
import java.awt.TextField;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.IOException;

/**
 * Secure Shell plugin for the Java Telnet Application. This is a plugin
 * to be used instead of Telnet for secure remote terminal sessions over
 * insecure networks. The implementation uses code that was derived from
 * </I>Cedric Gourio</I>'s implementation that used parts of the old Java
 * Telnet Applet. Have a look at the package de.mud.ssh for further information
 * about ssh or look at the official ssh homepage:
 * <A HREF="http://www.ssh.org/">http://www.ssh.org/</A>.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class SSH extends Plugin implements FilterPlugin {

  protected FilterPlugin source;
  protected SshIO handler;

  private final static int debug = 0;

  private boolean auth = false;

  /**
   * Create a new ssh plugin.
   */
  public SSH(PluginBus pbus) {
    super(pbus);
    // this is a jdk 1.1.x hack
    final PluginBus bus = pbus;

    // create a new telnet protocol handler
    handler = new SshIO() {
      /** get the current terminal type */
      public String getTerminalType() {
        return (String)bus.broadcast(new TerminalTypeRequest());
      }
      /** get the current window size */
      public Dimension getWindowSize() {
        return (Dimension)bus.broadcast(new WindowSizeRequest());
      }
      /** notify about local echo */
      public void setLocalEcho(boolean echo) {
        bus.broadcast(new LocalEchoRequest(echo));
      }
      /** write data to our back end */
      public void write(byte[] b) throws IOException {
        source.write(b);
      }
    };

    // reset the protocol handler just in case :-)
    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        final Frame frame = new Frame("SSH User Authentication");
        Panel panel = new Panel(new GridLayout(2,2));
	final TextField login = new TextField(10);
	final TextField passw = new TextField(10);
	passw.setEchoChar('*');
	panel.add(new Label("User name")); panel.add(login);
	panel.add(new Label("Password")); panel.add(passw);
	frame.add("Center", panel);
	panel = new Panel();
	Button cancel = new Button("Cancel");
	Button ok = new Button("Login");
	ok.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent evt) {
	    handler.setLogin(login.getText());
	    handler.setPassword(passw.getText());
	    frame.dispose();
	    auth = true;
	  }
	});
	cancel.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent evt) {
	    frame.dispose();
	  }
	});
	panel.add(cancel);
	panel.add(ok);
	frame.add("South", panel);

	frame.pack();
	frame.show();
      }
      public void offline() {
        // handler.reset();
      }
    });
  }

  public void setFilterSource(FilterPlugin source) { 
    if(debug>0) System.err.println("ssh: connected to: "+source);
    this.source = source;
  }

  private byte buffer[];
  private int pos;

  public int read(byte[] b) throws IOException {
    while(!auth) try {
      Thread.sleep(1000);
    } catch(InterruptedException e) {
      e.printStackTrace();
    }

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
        
    int n = source.read(b);
    if(n > 0) {
      byte[] tmp = new byte[n];
      System.arraycopy(b, 0, tmp, 0, n);
      buffer = handler.handleSSH(tmp);
      if(buffer != null && buffer.length > 0) {
        if(debug > 0) 
	  System.err.println("ssh: incoming="+n+" now="+buffer.length);
	int amount = buffer.length <= b.length ?  buffer.length : b.length;
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
    if(!auth) return;
    handler.sendData(new String(b));
  }
}
