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

import de.mud.jta.FilterPlugin;
import de.mud.jta.Plugin;
import de.mud.jta.PluginBus;
import de.mud.jta.PluginConfig;
import de.mud.jta.VisualPlugin;
import de.mud.jta.event.ConfigurationListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;

/**
 * A capture plugin that captures data and stores it in a
 * defined location. The location is specified as a plugin
 * configuration option Capture.url and can be used in
 * conjunction with the UploadServlet from the tools directory.
 * <P>
 * Parametrize the plugin carefully:<br>
 * <b>Capture.url</b> should contain a unique URL can may have
 * parameters for identifying the upload.<br>
 * <i>Example:</i> http://mg.mud.de/servlet/UpladServlet?id=12345
 * <p>
 * The actually captured data will be appended as the parameter
 * <b>content</b>.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class Capture extends Plugin
        implements FilterPlugin, VisualPlugin,ActionListener {

  // this enables or disables the compilation of menu entries
  private final static boolean personalJava = false;

  // for debugging output
  private final static int debug = 1;

  /** The remote storage URL */
  protected Hashtable remoteUrlList = new Hashtable();

  /** The plugin menu */
  protected Menu menu;
  protected Dialog dialog;

  /** Whether the capture is currently enabled or not */
  protected boolean captureEnabled = false;

  // menu entries and the viewing frame/textarea
  private MenuItem start, stop, clear, save;
  private Frame frame;
  private TextArea textArea;

  /**
   * Initialize the Capture plugin. This sets up the menu entries
   * and registers the plugin on the bus.
   */
  public Capture(final PluginBus bus, final String id) {
    super(bus, id);

    if (!personalJava) {

      // set up viewing frame
      frame = new Frame("Java Telnet Applet: Captured Text");
      frame.setLayout(new BorderLayout());
      frame.add(textArea = new TextArea(24, 80), BorderLayout.CENTER);
      textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          frame.setVisible(false);
        }
      });
      frame.pack();

      // an error dialogue, in case the upload fails
      dialog = new Dialog(frame);
      dialog.setLayout(new BorderLayout());
      dialog.add(new Label("Cannot store data on remote server!"));
      Button close = new Button("Close Dialog");
      dialog.add(close);
      close.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dialog.setVisible(false);
        }
      });

      // set up menu entries
      menu = new Menu("Capture");
      start = new MenuItem("Start");
      start.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (debug > 0) System.out.println("Capture: start capturing");
          captureEnabled = true;
          start.setEnabled(false);
          stop.setEnabled(true);
        }
      });
      menu.add(start);

      stop = new MenuItem("Stop");
      stop.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (debug > 0) System.out.println("Capture: stop capturing");
          captureEnabled = false;
          start.setEnabled(true);
          stop.setEnabled(false);

        }
      });
      stop.setEnabled(false);
      menu.add(stop);

      clear = new MenuItem("Clear");
      clear.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (debug > 0) System.out.println("Capture: cleared captured text");
          textArea.setText("");
        }
      });
      menu.add(clear);
      menu.addSeparator();

      MenuItem view = new MenuItem("View/Hide Text");
      view.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (debug > 0) System.out.println("view/hide text: " + frame.isVisible());
          if (frame.isVisible()) {
            frame.setVisible(false);
            frame.hide();
          } else {
            frame.setVisible(true);
            frame.show();
          }
        }
      });
      menu.add(view);

    } // !personalJava


    // configure the remote URL
    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig config) {
        String tmp;
        int i = 1;
        while ((tmp = config.getProperty("Capture", id, i+".url")) != null) {
          try {
            String urlID = "URL."+i;
            URL remoteURL = new URL(tmp);
            remoteUrlList.put(urlID, remoteURL);
            if((tmp = config.getProperty("Capture", id, i+".params")) != null) {
              remoteUrlList.put(urlID+".params", tmp);
            }
            // use name if applicable or URL
            if((tmp = config.getProperty("Capture", id, i+".name")) != null) {
              save = new MenuItem("Save As "+tmp);
            } else {
              save = new MenuItem("Save As "+remoteURL.toString());
            }
            // enable menu entry
            save.setEnabled(true);
            save.addActionListener(Capture.this);
            save.setActionCommand(urlID);
            menu.add(save);
            // count up
            i++;
          } catch (MalformedURLException e) {
            System.err.println("capture url invalid: " + e);
          }
        }
      }
    });
  }

  public void actionPerformed(ActionEvent e) {
    String urlID = e.getActionCommand();
    URL url = (URL)remoteUrlList.get(urlID);

    if (debug > 0) System.err.println("Capture: storing text: "
                                      + urlID+": "
                                      + remoteUrlList.get(urlID));
    try {
      URLConnection urlConnection = url.openConnection();
      DataOutputStream out;
      DataInputStream in;

      // Let the RTS know that we want to do output.
      urlConnection.setDoInput(true);
      // Let the RTS know that we want to do output.
      urlConnection.setDoOutput(true);
      // No caching, we want the real thing.
      urlConnection.setUseCaches(false);
      // Specify the content type.
      urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      // retrieve extra arguments
      // Send POST output.
      // send the data to the url receiver ...
      out = new DataOutputStream(urlConnection.getOutputStream());
      String content = (String)remoteUrlList.get(urlID+".param");
      content =  (content == null ? "" : content + "&") + "content=" + URLEncoder.encode(textArea.getText());
      if(debug > 0) System.err.println("Capture: " + content);
      out.writeBytes(content);
      out.flush();
      out.close();

      // retrieve response from the remote host and display it.
      if(debug > 0) System.err.println("Capture: reading response");
      in = new DataInputStream(urlConnection.getInputStream());
      String str;
      while (null != ((str = in.readLine()))) {
        System.out.println("Capture: "+str);
      }
      in.close();

    } catch (IOException ioe) {
      dialog.setVisible(true);
      System.err.println("Capture: cannot store text on remote server: " + url );
      ioe.printStackTrace();
    }
    if (debug > 0) System.err.println("Capture: storage complete: " + url);
  }

  // this is where we get the data from (left side in plugins list)
  protected FilterPlugin source;

  /**
   * The filter source is the plugin where Capture is connected to.
   * In the list of plugins this is the one to the left.
   * @param source the next plugin
   */
  public void setFilterSource(FilterPlugin source) {
    if (debug > 0) System.err.println("Capture: connected to: " + source);
    this.source = source;
  }

  /**
   * Read data from the left side plugin, capture the content and
   * pass it on to the next plugin which called this method.
   * @param b the buffer to store data into
   */
  public int read(byte[] b) throws IOException {
    int size = source.read(b);
    if (captureEnabled && size > 0) {
      String tmp = new String(b, 0, size);
      textArea.append(tmp);
    }
    return size;
  }

  /**
   * Write data to the backend but also append it to the capture buffer.
   * @param b the buffer with data to write
   */
  public void write(byte[] b) throws IOException {
    if (captureEnabled) {
      textArea.append(new String(b));
    }
    source.write(b);
  }

  /**
   * The Capture plugin has no visual component that is embedded in
   * the JTA main frame, so this returns null.
   * @return always null
   */
  public Component getPluginVisual() {
    return null;
  }

  /**
   * The Capture menu for the menu bar as configured in the constructor.
   * @return the drop down menu
   */
  public Menu getPluginMenu() {
    return menu;
  }

}
