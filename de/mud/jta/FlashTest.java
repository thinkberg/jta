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
import java.io.IOException;
import java.io.InputStream;
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
    if (debug > 0) System.err.println("FlashTest: main(" + args + ")");
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
          if (localecho) {
            emulation.putString(new String(b) + "\r");
          }
          telnet.transpose(b);
        } catch (IOException e) {
          System.err.println("FlashTest: error sending data: " + e);
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
        System.err.println("FlashTest: EOR");
        terminal.redraw();
      }

      /** write data to our back end */
      public void write(byte[] b) throws IOException {
        System.err.println("FlashTest: writing " + Integer.toHexString(b[0]) + " " + new String(b));
        os.write(b);
      }
    };

    try {
      System.err.println("FlashTest: trying to connect " + host + " " + port);
      // open new socket and get streams
      socket = new Socket(host, Integer.parseInt(port));
      is = socket.getInputStream();
      os = socket.getOutputStream();

      reader = new Thread(this);
      running = true;
      reader.start();

    } catch (Exception e) {
      System.err.println("FlashTest: error connecting: " + e);
      e.printStackTrace();
      stop();
    }

    while (true) {
      System.err.println("FlashTest: Terminal restarted ...");
      terminal.start();
    }
  }

  boolean running = false;

  /**
   * Stop the applet and disconnect.
   */
  public void stop() {
    if (debug > 0)
      System.err.println("FlashTest: stop()");
    // when applet stops, disconnect
    if (socket != null) {
      try {
        socket.close();
      } catch (Exception e) {
        System.err.println("FlashTest: could not cleanly disconnect: " + e);
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
    if (debug > 0) System.err.println("FlashTest: run()");

    byte[] b = new byte[4096];
    int n = 0;
    while (running && n >= 0) {
      try {
        n = telnet.negotiate(b);	// we still have stuff buffered ...
        if (n > 0)
          emulation.putString(new String(b, 0, n));

        while (true) {
          n = is.read(b);
          System.err.println("FlashTest: got " + n + " bytes");
          if (n <= 0)
            emulation.putString(new String(b, 0, n));

          telnet.inputfeed(b, n);
          n = 0;
          while (true) {
            n = telnet.negotiate(b);
            if (n > 0)
              emulation.putString(new String(b, 0, n));
            if (n == -1) // buffer empty.
              break;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        stop();
        break;
      }
    }
    System.err.println("FlashTest: finished reading from remote host");
  }
}
