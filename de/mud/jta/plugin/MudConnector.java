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
import de.mud.jta.VisualPlugin;
import de.mud.jta.PluginBus;
import de.mud.jta.PluginConfig;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.SocketListener;
import de.mud.jta.event.SocketRequest;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

import java.util.Hashtable;

import java.net.URL;

import java.awt.Graphics;
import java.awt.Panel;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Component;
import java.awt.Menu;
import java.awt.Dimension;
import java.awt.Button;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class MudConnector extends Plugin implements VisualPlugin, Runnable {

  /** debugging level */
  private final static int debug = 0;

  protected URL listURL = null;
  protected Hashtable mudList = null;
  protected List mudListSelector = new List();
  protected TextField mudName, mudAddr, mudPort;
  protected Button connect;
  protected Panel mudListPanel;
  protected CardLayout layouter;
  protected ProgressBar progress;
  protected Label errorLabel;

  /**
   * Implementation of a progress bar to display the progress of
   * loading the mud list.
   */
  class ProgressBar extends Component {
    int max, current;
    String text;
    Dimension size = new Dimension(250, 20);

    public void setMax(int max) {
      this.max = max;
    }

    public synchronized void paint(Graphics g) {
      int width = (int) (((float)current/(float)max) * getSize().width);
      g.fill3DRect(0, 0, getSize().width, getSize().height, false);
      g.setColor(getBackground());
      g.fill3DRect(0, 0, width, getSize().height, true);
      g.setColor(getForeground());
      g.setXORMode(getBackground());
      g.drawString(""+(current * 100 / (max>0?max:1))+"%", 
                   getSize().width/2 - 15, getSize().height / 2);
      g.drawString(text,
                   getSize().width/2 - 
		     getFontMetrics(getFont()).stringWidth(text) / 2, 
		   getSize().height / 2 + 12);
    }

    public synchronized void adjust(int value, String name) {
      if((current = value) > max)
        current = max;
      text = name;
      repaint();
    }

    public void setSize(int width, int height) {
      size = new Dimension(width, height);
    }

    public Dimension getPreferredSize() { return size; }
    public Dimension getMinimumSize() { return size; }
  }
      

  /**
   * Create the list plugin and get the url to the actual list.
   */
  public MudConnector(final PluginBus bus, final String id) {
    super(bus, id);

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig config) {
        String url = 
	  config.getProperty("MudConnector", id, "listURL");
        if(url != null) {
          try {
            listURL = new URL(url);
          } catch(Exception e) {
            MudConnector.this.error(""+e);
	    errorLabel.setText("Error: "+e);
          } 
        } else {
          MudConnector.this.error("no listURL specified");
	  errorLabel.setText("Missing list URL");
	  layouter.show(mudListPanel, "ERROR");
	}
      }
    });

    bus.registerPluginListener(new SocketListener() {
      public void connect(String host, int port) { setup(); }
      public void disconnect() { setup(); }
    });
    mudListPanel = new Panel(layouter = new CardLayout()) {
      public void update(java.awt.Graphics g) { 
        paint(g);
      }
    };

    mudListPanel.add("ERROR", errorLabel = new Label("Loading ..."));
    Panel panel = new Panel(new BorderLayout());
    panel.add("North", new Label("Loading mud list ... please wait"));
    panel.add("Center", progress = new ProgressBar());
    mudListPanel.add("PROGRESS", panel);
    panel = new Panel(new BorderLayout());
    panel.add("Center", mudListSelector);
    mudListPanel.add("MUDLIST", panel);
    panel.add("East", panel = new Panel(new GridLayout(3, 1)));
    panel.add(mudName = new TextField(20));
    mudName.setEditable(false);
    Panel apanel = new Panel(new BorderLayout());
    apanel.add("Center", mudAddr = new TextField(20));
    mudAddr.setEditable(false);
    apanel.add("East", mudPort = new TextField(6));
    mudPort.setEditable(false);
    panel.add(apanel);
    panel.add(connect = new Button("Connect"));

    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        String addr = mudAddr.getText();
	String port = mudPort.getText();
	if(addr != null) {
	  bus.broadcast(new SocketRequest());
	  if(port == null || port.length() <= 0)
	    port = "23";
	  bus.broadcast(new SocketRequest(addr, Integer.parseInt(port)));
	}
      }
    };

    connect.addActionListener(al);
    mudListSelector.addActionListener(al);

    mudListSelector.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent evt) {
        switch(evt.getStateChange()) {
	  case ItemEvent.SELECTED:
	    String item = (String)mudListSelector.getSelectedItem();
	    mudName.setText(item);
	    Object mud[] = (Object[])mudList.get(item);
	    mudAddr.setText((String)mud[0]);
	    mudPort.setText(((Integer)mud[1]).toString());
	    break;
	  case ItemEvent.DESELECTED:
	    mudName.setText("");
	    mudAddr.setText("");
	    mudPort.setText("");
	    break;
	}
      }
    });



    layouter.show(mudListPanel, "PROGRESS");
  }

  private void setup() {
    if(mudList == null && listURL != null)
      (new Thread(this)).start();
  }

  public void run() {
    try {
      mudList = new Hashtable();
      BufferedReader r = 
        new BufferedReader(new InputStreamReader(listURL.openStream()));

      String line = r.readLine();
      int mudCount = 0;
      try {
        mudCount = Integer.parseInt(line);
      } catch(NumberFormatException nfe) {
        error("number of muds: "+nfe);
      }
      System.out.println("MudConnector: expecting "+mudCount+" mud entries");
      progress.setMax(mudCount);

      StreamTokenizer ts = new StreamTokenizer(r);
      ts.resetSyntax();
      ts.whitespaceChars(0, 9);
      ts.ordinaryChars(32, 255);
      ts.wordChars(32, 255);

      String name, host;
      Integer port;
      int token, counter = 0;

      while((token = ts.nextToken()) != ts.TT_EOF) {
        name = ts.sval; 

        if((token = ts.nextToken()) != ts.TT_EOF) {
	  if(token == ts.TT_EOL)
	    error(name+": unexpected end of line"
	                      +", missing host and port");
          host = ts.sval;
          port = new Integer(23);
          if((token = ts.nextToken()) != ts.TT_EOF) try {
	    if(token == ts.TT_EOL)
	      error(name+": default port 23");
            port = new Integer(ts.sval);
          } catch(NumberFormatException nfe) {
            error("port for "+name+": "+nfe);
          }

          if(debug > 0) 
            error(name+" ["+host+","+port+"]");
          mudList.put(name, new Object[] { host, port });
          mudListSelector.add(name);
	  System.out.println("MudConnector: "+name);
	  progress.adjust(++counter, name);
	  mudListPanel.repaint();
        }
	while(token != ts.TT_EOF && token != ts.TT_EOL)
	  token = ts.nextToken();
      }
      System.out.println("MudConnector: found "+mudList.size()+" entries");
    } catch(Exception e) {
      error("error: "+e);
      errorLabel.setText("Error: "+e);
      layouter.show(mudListPanel, "ERROR");
    }
    layouter.show(mudListPanel, "MUDLIST");
  }

  public Component getPluginVisual() {
    return mudListPanel;
  }

  public Menu getPluginMenu() {
    return null;
  }
}
