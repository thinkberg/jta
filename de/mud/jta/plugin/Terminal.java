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
import de.mud.jta.VisualPlugin;
import de.mud.jta.PluginBus;

import de.mud.terminal.vt320;

import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.TerminalTypeListener;
import de.mud.jta.event.WindowSizeListener;
import de.mud.jta.event.LocalEchoListener;

import java.awt.Component;
import java.awt.Menu;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;

import java.io.IOException;

import java.util.Properties;
import java.util.Enumeration;

/**
 * The terminal plugin represents the actual terminal where the
 * data will be displayed and the gets the keyboard input to sent
 * back to the remote host,
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Terminal extends Plugin 
  implements FilterPlugin, VisualPlugin, Runnable {

  private final static int debug = 0;
  
  /** holds the actual terminal emulation */
  protected vt320 terminal;

  private Thread reader = null;

  /**
   * Create a new terminal plugin and initialize the terminal emulation.
   */
  public Terminal(PluginBus bus) {
    super(bus);
    terminal = new vt320() {
      public void write(byte[] b) {
        try {
	  Terminal.this.write(b);
	} catch(IOException e) {
	  reader = null;
	}
      }
    };

    // register an online status listener
    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        if(debug > 0) System.err.println("Terminal: online");
        if(reader == null) {
          reader = new Thread(Terminal.this);
          reader.start();
        }
      }

      public void offline() {
        if(debug > 0) System.err.println("Terminal: offline");
        if(reader != null)
          reader = null;
      }
    });

    bus.registerPluginListener(new TerminalTypeListener() {
      public String getTerminalType() {
        return terminal.getTerminalID();
      }
    });

    bus.registerPluginListener(new WindowSizeListener() {
      public Dimension getWindowSize() {
        return terminal.getScreenSize();
      }
    });

    bus.registerPluginListener(new LocalEchoListener() {
      public void setLocalEcho(boolean echo) {
        terminal.setLocalEcho(echo);
      }
    });

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(Properties config) {
        configure(config);
      }
    });
  }

  private void configure(Properties config) {
    Enumeration p = config.keys();

    while(p.hasMoreElements()) {
      String key = (String)p.nextElement();
      if(key.startsWith("Terminal.")) {
        if(key.equals("Terminal.foreground"))
	  terminal.setForeground(Color.decode(config.getProperty(key)));
	else if(key.equals("Terminal.background"))
	  terminal.setBackground(Color.decode(config.getProperty(key)));
	else if(key.equals("Terminal.colorSet"))
	  System.out.println("Terminal.colorSet not implemented yet");
	else if(key.equals("Terminal.borderRaised")) 
	  /* do nothing */ ;
	else if(key.equals("Terminal.border")) {
	  boolean raised = false;
	  if(config.containsKey("Terminal.borderStyle"))
	    raised = Boolean.getBoolean("Terminal.borderRaised");
	  terminal.setBorder(Integer.parseInt(config.getProperty(key)),
	                     raised);
        } else if(key.equals("Terminal.scrollBar"))
	  System.out.println("Terminal.scrollBar not implemented yet");
	else if(key.equals("Terminal.id"))
	  terminal.setTerminalID(config.getProperty(key));
	else if(key.equals("Terminal.buffer"))
	  terminal.setBufferSize(Integer.parseInt(config.getProperty(key)));
	else if(key.equals("Terminal.size"))
	  System.out.println("Terminal.size not implemented yet");
	else if(key.equals("Terminal.resize")) {
	  String resize = config.getProperty("Terminal.resize");
	  if(resize.equals("font"))
	    terminal.setResizeStrategy(terminal.RESIZE_FONT);
	  else if(resize.equals("screen"))
	    terminal.setResizeStrategy(terminal.RESIZE_SCREEN);
	  else 
	    terminal.setResizeStrategy(terminal.RESIZE_NONE);
        } else if(key.equals("Terminal.fontSize") || 
	          key.equals("Terminal.fontStyle"))
          /* do nothing */ ;
        else if(key.equals("Terminal.font")) {
	  int style = Font.PLAIN, fsize = 12;
	  if(config.containsKey("Terminal.fontSize"))
	    fsize = Integer.parseInt(config.getProperty("Terminal.fontSize"));
	  String fontStyle = config.getProperty("Terminal.fontStyle");
	  if(fontStyle == null || fontStyle.equals("plain"))
	    style = Font.PLAIN;
	  else if(fontStyle.equals("bold"))
	    style = Font.BOLD;
	  else if(fontStyle.equals("italic"))
	    style = Font.ITALIC;
	  terminal.setFont(new Font(config.getProperty(key), style, fsize));
	} else if(key.equals("Terminal.keyCodes")) 
	  System.out.println("Terminal.keyCodes not implemented yet");
	else if(key.equals("Terminal.VMS"))
	  terminal.setVMS(Boolean.getBoolean(config.getProperty(key)));
	else if(key.equals("Terminal.IBM"))
	  terminal.setIBMCharset(Boolean.getBoolean(config.getProperty(key)));
        else
	  System.err.println("Error: '"+key+"' is not a Terminal property");
      }
    }
  }
	  
  /**
   * Continuously read from our back end and display the data on screen.
   */
  public void run() {
    byte[] t, b = new byte[256];
    int n = 0;
    while(n >= 0) try {
      n = read(b);
      if(n > 0) terminal.putString(new String(b, 0, n));
    } catch(IOException e) {
      reader = null;
      return;
    }
  }

  protected FilterPlugin source;

  public void setFilterSource(FilterPlugin source) {
    if(debug > 0) System.err.println("Terminal: connected to: "+source);
    this.source = source;
  }

  public int read(byte[] b) throws IOException {
    return source.read(b);
  }

  public void write(byte[] b) throws IOException {
    source.write(b);
  }

  public Component getPluginVisual() {
    return terminal;
  }

  public Menu getPluginMenu() {
    return null;
  }
}
