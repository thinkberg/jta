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
import de.mud.jta.event.AppletListener;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

import java.net.URL;

import java.util.Vector;

import java.awt.Component;
import java.awt.List;
import java.awt.Menu;
import java.awt.Panel;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.applet.Applet;
import java.applet.AppletContext;

/**
 *
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class URLFilter extends Plugin 
  implements FilterPlugin, VisualPlugin, Runnable {

  /** debugging level */
  private final static int debug = 0;

  protected List urlList = new List(4, false);
  protected Panel urlPanel;
  protected Menu urlMenu;

  protected PipedInputStream pin;
  protected PipedOutputStream pout;

  protected AppletContext context;


  /**
   * Create a new scripting plugin.
   */
  public URLFilter(PluginBus bus, final String id) {
    super(bus, id);

    urlPanel = new Panel(new BorderLayout());
    urlList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        showURL(evt.getActionCommand());
      }
    });
    urlPanel.add("Center", urlList);
    Panel p = new Panel(new GridLayout(3,1));
    Button b = new Button("Clear List");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        urlList.removeAll();
      }
    });
    p.add(b);
    b = new Button("Remove URL");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	String item = urlList.getSelectedItem();
        if(item != null) urlList.remove(item);
      }
    });
    p.add(b);
    b = new Button("Show URL");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	String item = urlList.getSelectedItem();
        if(item != null) showURL(item);
      }
    });
    p.add(b);
    urlPanel.add("East", p);

    bus.registerPluginListener(new AppletListener() {
      public void setApplet(Applet applet) {
        context = applet.getAppletContext();
      }
    });

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig config) {
      }
    });

    // create the recognizer pipe
    pin = new PipedInputStream();
    pout = new PipedOutputStream();

    try {
      pout.connect(pin);
    } catch(IOException e) {
      System.err.println("URLFilter: error installing recognizer: "+e);
    }

    // start the recognizer
    Thread recognizer = new Thread(this);
    recognizer.start();
  }

  public void run() {
    try {
      StreamTokenizer st = 
        new StreamTokenizer(new BufferedReader(new InputStreamReader(pin)));
      st.eolIsSignificant(false);
      st.slashSlashComments(false);
      st.slashStarComments(false);
      st.whitespaceChars(0, 31);
      st.wordChars(32, 127);
      st.ordinaryChar('<');
      st.ordinaryChar('>');
      st.ordinaryChar('"');

      int token;
      while((token = st.nextToken()) != StreamTokenizer.TT_EOF) {
        if(token == StreamTokenizer.TT_WORD) {
          String word = st.sval.toLowerCase();
          if(word.startsWith("http://") ||
	     word.startsWith("ftp://") ||
	     word.startsWith("gopher://") ||
	     word.startsWith("file://")) {
	    urlList.add(st.sval);
	    System.out.println("URLFilter: found \""+st.sval+"\"");
	  }
        }
      }
    } catch(IOException e) {
      System.err.println("URLFilter: recognition aborted: "+e);
    }
  }

  protected void showURL(String url) {
    if(context == null) return;
    try {
      context.showDocument(new URL(url), "URLFilter");
    } catch(Exception e) {
      System.err.println("URLFilter: cannot load url: "+e);
    }
  }

  /** holds the data source for input and output */
  protected FilterPlugin source;

  /**
   * Set the filter source where we can read data from and where to
   * write the script answer to.
   * @param plugin the filter plugin we use as source
   */
  public void setFilterSource(FilterPlugin plugin) {
    source = plugin;
  }

  /**
   * Read an array of bytes from the back end and send it to the
   * url parser to see if it matches. 
   * @param b the array where to read the bytes in
   * @return the amount of bytes actually read
   */
  public int read(byte[] b) throws IOException {
    int n = source.read(b);
    if(n > 0) pout.write(b, 0, n);
    return n;
  }

  public void write(byte[] b) throws IOException {
    source.write(b);
  }

  public Component getPluginVisual() {
    return urlPanel;
  }

  public Menu getPluginMenu() {
    return urlMenu;
  }
}
