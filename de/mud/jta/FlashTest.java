/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
 *
 * Please visit http://javatelnet.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */
package de.mud.jta;

import de.mud.telnet.TelnetProtocolHandler;
import de.mud.terminal.FlashTerminal;
import de.mud.terminal.vt320;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * <B>Small Telnet Applet implementation</B><P>
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class FlashTest implements Runnable {

  private final static int debug = 3;

  /** hold the host and port for our connection */
  private String host, port;

  /** hold the socket */
  private Socket socket;
  private InputStream is;
  private OutputStream os;

  private Thread reader;

  /** the terminal */
  private vt320 emulation;
  private FlashTerminal terminal;

  /** the telnet protocol handler */
  private TelnetProtocolHandler telnet;

  private boolean localecho = true;

  /**
   * Read all parameters from the applet configuration and
   * do initializations for the plugins and the applet.
   */
  public static void main(String args[]) {
    if (debug > 0) System.err.println("jta: init()");
    new FlashTest(args[0], args[1]);
  }

  public FlashTest(String host, String port) {
    this.host = host;
    this.port = port;

    // we now create a new terminal that is used for the system
    // if you want to configure it please refer to the api docs
    emulation = new vt320() {
      /** before sending data transform it using telnet (which is sending it) */
      public void write(byte[] b) {
        try {
          if (localecho)
            emulation.putString(new String(b));
          telnet.transpose(b);
        } catch (IOException e) {
          System.err.println("jta: error sending data: " + e);
        }
      }
    };

    terminal = new FlashTerminal();
    terminal.setVDUBuffer(emulation);

    // then we create the actual telnet protocol handler that will negotiate
    // incoming data and transpose outgoing (see above)
    telnet = new TelnetProtocolHandler() {
      /** get the current terminal type */
      public String getTerminalType() {
        return emulation.getTerminalID();
      }

      /** get the current window size */
      public Dimension getWindowSize() {
        return new Dimension(emulation.getColumns(), emulation.getRows());
      }

      /** notify about local echo */
      public void setLocalEcho(boolean echo) {
        localecho = true;
      }

      /** notify about EOR end of record */
      public void notifyEndOfRecord() {
        // only used when EOR needed, like for line mode
      }

      /** write data to our back end */
      public void write(byte[] b) throws IOException {
        System.err.println("writing: "+new String(b));
        os.write(b);
      }
    };

    start();
  }

  boolean running = false;

  /**
   * Start the applet. Connect to the remote host.
   */
  public void start() {
    if (debug > 0)
      System.err.println("jta: start()");
    // disconnect if we are already connected
    if (socket != null) stop();

    try {
      // open new socket and get streams
      socket = new Socket(host, Integer.parseInt(port));
      is = socket.getInputStream();
      os = socket.getOutputStream();

      reader = new Thread(this);
      running = true;
      reader.start();

      Thread writer = new Thread() {
        public void run() {
          BufferedReader keyb = new BufferedReader(new InputStreamReader(System.in));
          String line;
          try {
            while ((line = keyb.readLine()) != null) {
              System.err.println("got: "+line);
              emulation.putString(line+"\r");
              telnet.transpose((line+"\r").getBytes());
            }
          } catch (IOException e) {
            System.err.println("end of keyboard input");
          }
        }
      };
      writer.start();

    } catch (Exception e) {
      System.err.println("jta: error connecting: " + e);
      e.printStackTrace();
      stop();
    }
  }

  /**
   * Stop the applet and disconnect.
   */
  public void stop() {
    if (debug > 0)
      System.err.println("jta: stop()");
    // when applet stops, disconnect
    if (socket != null) {
      try {
        socket.close();
      } catch (Exception e) {
        System.err.println("jta: could not cleanly disconnect: " + e);
      }
      socket = null;
      try {
        running = false;
      } catch (Exception e) {
        // ignore
      }
      reader = null;
    }
  }

  /**
   * Continuously read from remote host and display the data on screen.
   */
  public void run() {
    if (debug > 0)
      System.err.println("jta: run()");
    byte[] b = new byte[4096];
    int n = 0;
    while (running && n >= 0) {
      try {
        do {
          System.err.println("negotiating: "+n);
          n = telnet.negotiate(b);
          if (debug > 0 && n > 0)
            System.err.println("jta: \"" + (new String(b, 0, n)) + "\"");
          if (n > 0) emulation.putString(new String(b, 0, n));
        } while (running && n > 0);
        System.err.println("waiting for input ...." +is.available());
        n = is.read(b);
        System.err.println("n="+n);
        telnet.inputfeed(b, n);
      } catch (IOException e) {
        e.printStackTrace();
        stop();
        break;
      }
      System.err.println("check: "+running+" n="+n);
    }
    System.err.println("THE END");
  }

}
