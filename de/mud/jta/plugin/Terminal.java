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
import de.mud.jta.PluginConfig;
import de.mud.jta.FilterPlugin;
import de.mud.jta.VisualTransferPlugin;
import de.mud.jta.PluginBus;

import de.mud.terminal.vt320;

import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.TerminalTypeListener;
import de.mud.jta.event.WindowSizeListener;
import de.mud.jta.event.LocalEchoListener;
import de.mud.jta.event.FocusStatus;

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

import java.util.Properties;
import java.util.Hashtable;

/**
 * The terminal plugin represents the actual terminal where the
 * data will be displayed and the gets the keyboard input to sent
 * back to the remote host.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Terminal extends Plugin 
  implements FilterPlugin, VisualTransferPlugin, ClipboardOwner, Runnable {

  private final static boolean personalJava = false;

  private final static int debug = 0;
  
  /** holds the actual terminal emulation */
  protected vt320 terminal;

  /** the terminal panel that is displayed on-screen */
  protected Panel tPanel;

  /** holds the terminal menu */
  protected Menu menu;

  private Thread reader = null;

  /**
   * Create a new terminal plugin and initialize the terminal emulation.
   */
  public Terminal(final PluginBus bus, final String id) {
    super(bus, id);

    if(!personalJava) {

    menu = new Menu("Terminal");
    MenuItem item;
    menu.add(item = new MenuItem("Smaller Font"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Font font = terminal.getFont();
	terminal.setFont(new Font(font.getName(), 
	                     font.getStyle(), font.getSize()-1));
	if(tPanel.getParent() != null) {
	  Component parent = tPanel.getParent();
	  if(parent instanceof java.awt.Frame) 
	    ((java.awt.Frame)parent).pack();
	  tPanel.getParent().doLayout();
	  tPanel.getParent().validate();
	}
      }
    });
    menu.add(item = new MenuItem("Larger Font"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Font font = terminal.getFont();
	terminal.setFont(new Font(font.getName(), 
	                     font.getStyle(), font.getSize()+1));
	if(tPanel.getParent() != null) {
	  Component parent = tPanel.getParent();
	  if(parent instanceof java.awt.Frame) 
	    ((java.awt.Frame)parent).pack();
	  tPanel.getParent().doLayout();
	  tPanel.getParent().validate();
	}
      }
    });
    menu.add(item = new MenuItem("Buffer +50"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        terminal.setBufferSize(terminal.getBufferSize() + 50);
      }
    });
    menu.add(item = new MenuItem("Buffer -50"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        terminal.setBufferSize(terminal.getBufferSize() - 50);
      }
    });
    menu.addSeparator();
    menu.add(item = new MenuItem("Reset Terminal"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        terminal.reset();
      }
    });
    
    } // !personalJava

    // create the terminal emulation
    terminal = new vt320() {
      public void write(byte[] b) {
        try {
	  Terminal.this.write(b);
	} catch(IOException e) {
	  reader = null;
	}
      }
    };

    // the container for our terminal must use double-buffering
    // or at least reduce flicker by overloading update()
    tPanel = new Panel(new BorderLayout()) {
      public void update(java.awt.Graphics g) {
        paint(g);
      }
    };
    tPanel.add("Center", terminal);

    terminal.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent evt) {
	terminal.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        bus.broadcast(new FocusStatus(Terminal.this, evt));
      }
      public void focusLost(FocusEvent evt) {
	terminal.setCursor(Cursor.getDefaultCursor());
        bus.broadcast(new FocusStatus(Terminal.this, evt));
      }
    });

    // register an online status listener
    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        if(debug > 0) System.err.println("Terminal: online "+reader);
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
      public void setConfiguration(PluginConfig config) {
        configure(config);
      }
    });
  }

  private void configure(PluginConfig cfg) {
    String tmp;
    if((tmp = cfg.getProperty("Terminal", id, "foreground")) != null)
      terminal.setForeground(Color.decode(tmp));
    if((tmp = cfg.getProperty("Terminal", id, "background")) != null)
      terminal.setBackground(Color.decode(tmp));
 
    if((tmp = cfg.getProperty("Terminal", id, "colorSet")) != null) {
      Properties colorSet = new Properties();

      try {
        colorSet.load(getClass().getResourceAsStream(tmp));
      } catch(Exception e) {
        try {
          colorSet.load(new URL(tmp).openStream());
        } catch(Exception ue) {
	  error("cannot find colorSet: "+tmp);
	  error("resource access failed: "+e);
          error("URL access failed: "+ue);
	  colorSet = null;
        }
      }

      Hashtable colors = new Hashtable();
      colors.put("black", Color.black);
      colors.put("red", Color.red);
      colors.put("green", Color.green);
      colors.put("yellow", Color.yellow);
      colors.put("blue", Color.blue);
      colors.put("magenta", Color.magenta);
      colors.put("orange", Color.orange);
      colors.put("pink", Color.pink);
      colors.put("cyan", Color.cyan);
      colors.put("white", Color.white);
      colors.put("gray", Color.white);
        

      if(colorSet != null) {
        Color set[] = terminal.getColorSet();
        for(int i = 0; i < 8; i++)
          if((tmp = colorSet.getProperty("color"+i)) != null)
	    if(colors.get(tmp) != null)
	      set[i] = (Color)colors.get(tmp);
	    else try {
	      if(Color.getColor(tmp) != null) 
	        set[i] = Color.getColor(tmp);
              else
	        set[i] = Color.decode(tmp);
	    } catch(Exception e) {
	      error("ignoring unknown color code: "+tmp);
	    }
        terminal.setColorSet(set);
      }
    }
 
    if((tmp = cfg.getProperty("Terminal", id, "border")) != null) {
      String size = tmp;
      boolean raised = false;
      if((tmp = cfg.getProperty("Terminal", id, "borderRaised")) != null)
        raised = Boolean.valueOf(tmp).booleanValue();
      terminal.setBorder(Integer.parseInt(size), raised);
    }
 
    if((tmp = cfg.getProperty("Terminal", id, "scrollBar")) != null && 
       !personalJava) {
      String direction = tmp;
      if(!direction.equals("none")) {
        if(!direction.equals("East") && !direction.equals("West"))
          direction = "East";
        Scrollbar scrollBar = new Scrollbar();
        tPanel.add(direction, scrollBar);
        terminal.setScrollbar(scrollBar);
      }
    }

    if((tmp = cfg.getProperty("Terminal", id, "id")) != null)
      terminal.setTerminalID(tmp);

    if((tmp = cfg.getProperty("Terminal", id, "buffer")) != null) 
      terminal.setBufferSize(Integer.parseInt(tmp));

    if((tmp = cfg.getProperty("Terminal", id, "size")) != null) try {
      int idx = tmp.indexOf(',');
      int width = Integer.parseInt(tmp.substring(1, idx).trim());
      int height = Integer.parseInt(tmp.substring(idx+1,tmp.length()-1).trim());
      terminal.setScreenSize(width, height);
    } catch(Exception e) {
      error("screen size is wrong: "+tmp);
      error("error: "+e);
    }

    if((tmp = cfg.getProperty("Terminal", id, "resize")) != null)
      if(tmp.equals("font"))
	terminal.setResizeStrategy(terminal.RESIZE_FONT);
      else if(tmp.equals("screen"))
        terminal.setResizeStrategy(terminal.RESIZE_SCREEN);
      else 
        terminal.setResizeStrategy(terminal.RESIZE_NONE);
        
	
    if((tmp = cfg.getProperty("Terminal", id, "font")) != null) {
      String font = tmp;
      int style = Font.PLAIN, fsize = 12;
      if((tmp = cfg.getProperty("Terminal", id, "fontSize")) != null)
        fsize = Integer.parseInt(tmp);
      String fontStyle = cfg.getProperty("Terminal", id, "fontStyle");
      if(fontStyle == null || fontStyle.equals("plain"))
        style = Font.PLAIN;
      else if(fontStyle.equals("bold"))
        style = Font.BOLD;
      else if(fontStyle.equals("italic"))
        style = Font.ITALIC;
      else if(fontStyle.equals("bold+italic"))
        style = Font.BOLD | Font.ITALIC;
      terminal.setFont(new Font(font, style, fsize));
    }

    if((tmp = cfg.getProperty("Terminal", id, "keyCodes")) != null) {
      Properties keyCodes = new Properties();

      try {
        keyCodes.load(getClass().getResourceAsStream(tmp));
      } catch(Exception e) {
        try {
        keyCodes.load(new URL(tmp).openStream());
        } catch(Exception ue) {
          error("cannot find keyCodes: "+tmp);
	  error("resource access failed: "+e);
          error("URL access failed: "+ue);
	  keyCodes = null;
        }
      }

      // set the key codes if we got the properties
      if(keyCodes != null) 
        terminal.setKeyCodes(keyCodes);
    }

    if((tmp = cfg.getProperty("Terminal", id, "VMS")) != null)
      terminal.setVMS((Boolean.valueOf(tmp)).booleanValue());
    if((tmp = cfg.getProperty("Terminal", id, "IBM")) != null)
      terminal.setIBMCharset((Boolean.valueOf(tmp)).booleanValue());


    tPanel.setBackground(terminal.getBackground());
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
      break;
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
    return tPanel;
  }

  public Menu getPluginMenu() {
    return menu;
  }

  public void copy(Clipboard clipboard) {
    StringSelection selection = new StringSelection(terminal.getSelection());
    clipboard.setContents(selection, this);
  }

  public void paste(Clipboard clipboard) {
    if(clipboard == null) return;
    Transferable t = clipboard.getContents(this);
    try {
      /*
      InputStream is =
        (InputStream)t.getTransferData(DataFlavor.plainTextFlavor);
      if(debug > 0) 
        System.out.println("Clipboard: available: "+is.available());
      byte buffer[] = new byte[is.available()];
      is.read(buffer);
      is.close();
      */
      byte buffer[] = 
        ((String)t.getTransferData(DataFlavor.stringFlavor)).getBytes();
      try {
        write(buffer);
      } catch(IOException e) {
        reader = null;
      }
    } catch(Exception e) {
      // ignore any clipboard errors
      if(debug > 0) e.printStackTrace();
    }
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    terminal.clearSelection();
  }
}
