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

package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.PluginConfig;
import de.mud.jta.FilterPlugin;
import de.mud.jta.VisualTransferPlugin;
import de.mud.jta.PluginBus;

import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.TerminalTypeListener;
import de.mud.jta.event.WindowSizeListener;
import de.mud.jta.event.LocalEchoListener;
import de.mud.jta.event.FocusStatus;
import de.mud.jta.event.ReturnFocusListener;
import de.mud.jta.event.AppletListener;
import de.mud.jta.event.SoundRequest;
import de.mud.jta.event.TelnetCommandRequest;

import java.awt.Component;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.Scrollbar;
import java.awt.Cursor;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * The terminal plugin represents the actual terminal where the
 * data will be displayed and the gets the keyboard input to sent
 * back to the remote host.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Sink extends Plugin 
  implements FilterPlugin, Runnable {

  private final static int debug = 0;
  
  private Thread reader = null;

  public Sink(final PluginBus bus, final String id) {
    super(bus, id);
    // register an online status listener
    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        if(debug > 0) System.err.println("Terminal: online "+reader);
        if(reader == null) {
          reader = new Thread();
          reader.start();
        }
      }

      public void offline() {
        if(debug > 0) System.err.println("Terminal: offline");
        if(reader != null)
          reader = null;
      }
    });
  }

  /**
   * Continuously read from our back end and drop the data.
   */
  public void run() {
    byte[] t, b = new byte[256];
    int n = 0;
    while(n >= 0) try {
      n = read(b);
      /* drop the bytes into the sink :) */
    } catch(IOException e) {
      reader = null;
      break;
    }
  }

  protected FilterPlugin source;

  public void setFilterSource(FilterPlugin source) {
    if(debug > 0) System.err.println("Terminal: connected to: "+source);
    this.source = source;
  }

  public FilterPlugin getFilterSource() {
    return source;
  }

  public int read(byte[] b) throws IOException {
    return source.read(b);
  }

  public void write(byte[] b) throws IOException {
    source.write(b);
  }
}
