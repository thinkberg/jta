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
package de.mud.flash;

import de.mud.telnet.TelnetProtocolHandler;
import de.mud.terminal.vt320;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;

/**
 * <B>Flash Terminal Server implementation</B><P>
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class FlashTerminalServer implements Runnable {

  private final static int debug = 3;

  /**
   * Read all parameters from the applet configuration and
   * do initializations for the plugins and the applet.
   */
  public static void main(String args[]) {
    System.out.println("FlashTerminalServer (c) 2002 Matthias L. Jugel, Marcus Meiﬂner");
    if(args.length < 2) {
      System.err.println("usage: FlashTerminalServer host port");
      System.exit(0);
    }
    if (debug > 0)
      System.err.println("FlashTerminalServer: main(" + args[0] + ", "+args[1] + ")");
    try {
      ServerSocket serverSocket = new ServerSocket(9999);
      // create a new
      while(true) {
        System.out.println("FlashTerminalServer: waiting for connection ...");
        Socket flashClientSocket = serverSocket.accept();
        new FlashTerminalServer(args[0], args[1], flashClientSocket);
      }
    } catch (IOException e) {
      System.err.println("FlashTerminalServer: error opening server socket: "+e);
    }
  }

  /** hold the socket */
  private Socket socket;
  private InputStream is;
  private OutputStream os;
  private boolean running;

  /** the terminal */
  private vt320 emulation;
  private FlashTerminal terminal;

  /** the telnet protocol handler */
  private TelnetProtocolHandler telnet;

  private boolean localecho = true;


  public FlashTerminalServer(String host, String port, Socket flashSocket) {

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
          System.err.println("FlashTerminalServer: error sending data: " + e);
        }
      }
    };

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
        if(debug > 0)
          System.err.println("FlashTerminalServer: EOR");
        terminal.redraw();
      }

      /** write data to our back end */
      public void write(byte[] b) throws IOException {
        if(debug > 0)
          System.err.println("FlashTerminalServer: writing " + Integer.toHexString(b[0]) + " " + new String(b));
        os.write(b);
      }
    };

    try {
      // open new socket and get streams
      socket = new Socket(host, Integer.parseInt(port));
      is = socket.getInputStream();
      os = socket.getOutputStream();

      (new Thread(this)).start();

      terminal = new FlashTerminal(flashSocket) {
        public void disconnect() {
          running = false;
        }
      };
      terminal.setVDUBuffer(emulation);
    } catch (IOException e) {
      System.err.println("FlashTerminalServer: error connecting to remote host: "+e);
    } catch (NumberFormatException e) {
      System.err.println("FlashTerminalServer: "+port+" is not a correct number");
    }
  }

  public void run() {
    if (debug > 0) System.err.println("FlashTerminalServer: run()");
    running = true;

    byte[] b = new byte[4096];
    int n = 0;
    while (running && n >= 0) {
      try {
        n = telnet.negotiate(b);	// we still have stuff buffered ...
        if (n > 0)
          emulation.putString(new String(b, 0, n));

        while (true) {
          n = is.read(b);
          System.err.println("FlashTerminalServer: got " + n + " bytes");
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
        break;
      }
    }
    System.err.println("FlashTerminalServer: finished reading from remote host");

  }
}