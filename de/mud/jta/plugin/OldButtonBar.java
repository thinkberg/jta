/**
 *
 * This file is part of "The Java Telnet Applet".
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * "The Java Telnet Applet" is distributed in the hope that it will be 
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
import de.mud.jta.FilterPlugin;
import de.mud.jta.VisualPlugin;
import de.mud.jta.event.ConfigurationListener;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Vector;

import java.net.URL;

import java.awt.Component;
import java.awt.Panel;
import java.awt.Button;
import java.awt.TextField;
import java.awt.Event;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <B>THIS IS THE SIMPLE PORT OF THE OLD BUTTON BAR!</B><P>
 * 
 * @version $Id$
 * @author  Matthias L. Jugel, Marcus Meiﬂner
 */
public class OldButtonBar extends Plugin 
  implements VisualPlugin, ActionListener {

  protected Panel panel = new Panel();

  // these tables contain our buttons and fields.
  private Hashtable buttons = null;
  private Hashtable fields = null;

  public OldButtonBar(PluginBus bus) {
    super(bus);

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(Properties cfg) {
        if(cfg.getProperty("OldButtonBar.setup") == null)
	  return;

        Properties config = new Properties();

	try {
	  config.load(new URL(cfg.getProperty("OldButtonBar.setup")).openStream());
	} catch(Exception e) {
	  System.err.println("OldButtonBar: error: "+e);
	  return;
	}
	
        String tmp; 
        int nr = 1;
        String button = null, input = null;
        while((button = config.getProperty(nr+"#Button")) != null ||
	      (input = config.getProperty(nr+"#Input")) != null) {
	  nr++;
	  if(button != null) {
	    if(buttons == null) buttons = new Hashtable();
	    int idx = button.indexOf('|');
	    if(button.length() == 0)
	      System.out.println("OldButtonBar: Button: no definition");
	    if(idx < 0 || idx == 0) {
	      System.out.println("OldButtonBar: Button: empty name \""
	                        +button+"\"");
	      continue;
	    }
	    if(idx == button.length() - 1) {
	      System.out.println("OldButtonBar: Button: empty command \""
	                        +button+"\"");
	      continue;
	    }
	    Button b = new Button(button.substring(0, idx));
	    buttons.put(b, button.substring(idx+1, button.length()));
	    b.addActionListener(OldButtonBar.this);
	    panel.add(b);
	  } else
	    if(input != null) {
	      if(fields == null) fields = new Hashtable();
	      int idx = input.indexOf('|');
	      if(input.length() == 0)
	        System.out.println("OldButtonBar: Input field: no definition");
	      if(idx < 0 || idx == 0) {
	        System.out.println("OldButtonBar: Input field: empty name \""
		                  +input+"\"");
	        continue;
	      }
	      int si, size;
	      if((si = input.indexOf('#', 0)) == 0) {
	        System.out.println("OldButtonBar: Input field: empty name");
	        continue;
	      }
	      if(si < 0 || si == idx-1) size = 10;
	      else size = Integer.parseInt(input.substring(si+1, idx));
	      TextField t = 
	        new TextField(input.substring(idx + 1, 
					      input.lastIndexOf('|') == idx ?
					      input.length() : 
					      (idx = input.lastIndexOf('|'))),
			      size);
	      buttons.put(t, input.substring(idx + 1, input.length()));
	      fields.put(input.substring(0, (si < 0 ? idx : si)), t);
	      t.addActionListener(OldButtonBar.this);
	      panel.add(t);
	    }
	  button = input = null;
        }
      }
    });
  }

  public void actionPerformed(ActionEvent evt) {
    String tmp;
    if((tmp = (String)buttons.get(evt.getSource())) != null) {
      System.out.println("OldButtonBar: "+tmp);
      String cmd = "", function = null;
      int idx = 0, oldidx = 0;
      while((idx = tmp.indexOf('\\', oldidx)) >= 0 && 
	    ++idx <= tmp.length()) {
	cmd += tmp.substring(oldidx, idx-1);
	switch(tmp.charAt(idx)) {
	case 'b': cmd += "\b"; break;
	case 'e': cmd += ""; break;
	case 'n': cmd += "\n"; break;
	case 'r': cmd += "\r"; break;
	case '$': {
	  int ni = tmp.indexOf('(', idx+1);
	  if(ni < idx) {
	    System.out.println("ERROR: Function: missing '('");
	    break;
	  }
	  if(ni == ++idx) {
	    System.out.println("ERROR: Function: missing name");
	    break;
	  }
	  function = tmp.substring(idx, ni);
	  idx = ni+1;
	  ni = tmp.indexOf(')', idx);
	  if(ni < idx) {
	    System.out.println("ERROR: Function: missing ')'");
	    break;
	  }
	  tmp = tmp.substring(idx, ni);
	  idx = oldidx = 0;
	  continue;
	}
	case '@': {
	  int ni = tmp.indexOf('@', idx+1);
	  if(ni < idx) {
	    System.out.println("ERROR: Input Field: '@'-End Marker not found");
	    break;
	  }
	  if(ni == ++idx) {
	    System.out.println("ERROR: Input Field: no name specified");
	    break;
	  }
	  String name = tmp.substring(idx, ni);
	  idx = ni;
	  TextField t;
	  if(fields == null || (t = (TextField)fields.get(name)) == null) {
	    System.out.println("ERROR: Input Field: requested input \""+
			       name+"\" does not exist");
	    break;
	  }
	  cmd += t.getText();
	  t.setText("");
	  break;
	}
	default : cmd += tmp.substring(idx, ++idx);
	}
	oldidx = ++idx;
      }

      if(oldidx <= tmp.length()) cmd += tmp.substring(oldidx, tmp.length());
      
      if(function != null) {
	if(function.equals("exit")) { 
	  try {
	    System.exit(0);
	  } catch(Exception e) { e.printStackTrace(); }
	}
	if(function.equals("connect")) {
	  String address = null;
	  int port = -1;
	  try {
	    if((idx = cmd.indexOf(",")) >= 0) {
	      try {
		port = Integer.parseInt(cmd.substring(idx+1, cmd.length()));
	      } catch(Exception e) {
		port = -1;
	      }
	      cmd = cmd.substring(0, idx);
	    }
	    if(cmd.length() > 0) address = cmd;
	    if(address != null) 
	      if(port != -1) System.err.println("config.connect(address, port);");
	      else System.err.println("config.connect(address);");
	    else System.err.println("config.connect();");
	  } catch(Exception e) {
	    System.err.println("OldButtonBar: connect(): failed");
	    e.printStackTrace();
	  }
	} else
	  if(function.equals("disconnect"))
	    System.err.println("Closed connection.");
	  else
	    if(function.equals("detach")) {
		System.err.println("not implemented");
	    }
	    else
	      System.out.println("ERROR: function not implemented: \""+
				 function+"\"");
      }
      // cmd += tmp.substring(oldidx, tmp.length());
      if(cmd.length() > 0) System.err.println("config.send(cmd);");
    }
  }

  public Component getPluginVisual() {
    return panel;
  }

  public Menu getPluginMenu() {
    return null;
  }
}

