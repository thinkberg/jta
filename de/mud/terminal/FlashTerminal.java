/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meißner 1996-2002. All Rights Reserved.
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

package de.mud.terminal;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;

public class FlashTerminal implements VDUDisplay, Runnable {
  private final static int debug = 1;
  private boolean simpleMode = true;
  private boolean terminalReady = false;
  private VDUBuffer buffer;
  private ServerSocket serverSocket;
  private BufferedWriter writer;
  private BufferedReader reader;

  /** A list of colors used for representation of the display */
  private String color[] = {"#000000",
                            "#ff0000",
                            "#00ff00",
                            "#ffff00",
                            "#0000ff",
                            "#ff00ff",
                            "#00ffff",
                            "#ffffff",
                            null, // bold color
                            null, // inverted color
  };


  public void start() {
    if (serverSocket == null) {
      try {
        serverSocket = new ServerSocket(9999);
      } catch (IOException e) {
        System.err.println("io exception while setting up server socket: " + e);
      }
    }

    try {
      if (debug > 0) System.err.println("FlashTerminal: waiting for flash connection ...");
      Socket s = serverSocket.accept();
      if (debug > 0) System.err.println("FlashTerminal: got connection: " + s);
      writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
      reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
      run();
    } catch (IOException e) {
      System.err.println("FlashTerminal: unable to accept connection: " + e);
    }
  }

  private void perf(String msg) {
    System.err.print(System.currentTimeMillis());
    System.err.println(" " + msg);
  }

  public void run() {
    buffer.update[0] = true;
    redraw();
    char buf[] = new char[1024];
    int n = 0;
    do {
      try {
        if (debug > 0) System.err.println("FlashTerminal: waiting for keyboard input ...");

        // read from flash frontend
        n = reader.read(buf);
        if (n > 0 && buf[0] == '<') {
          handleXMLCommand(new String(buf, 0, n - 1));
          continue;
        }
        if (debug > 0) System.err.println("FlashTerminal: got " + n + " keystokes: " + new String(buf, 0, n));

        if (n > 0 && (buffer instanceof VDUInput)) {
          if (simpleMode) {
            // in simple mode simply write the data to the remote host
            // we have to convert the chars to bytes ...
            byte tmp[] = new byte[n];
            for (int i = 0; i < n - 1; i++) {
              tmp[i] = (byte) buf[i];
            }
            ((VDUInput) buffer).write((byte[]) tmp);
          } else {
            // write each key for it's own
            for (int i = 0; i < n - 1; i++) {
              ((VDUInput) buffer).keyTyped((int) buf[i], buf[i], 0);
            }
          }
        }
      } catch (IOException e) {
        System.err.println("FlashTerminal: i/o exception reading keyboard input");
      }
    } while (n >= 0);
    if (debug > 0) System.err.println("FlashTerminal: end of keyboard input");
  }

  private SAXBuilder builder = new SAXBuilder();

  private void handleXMLCommand(String xml) {
    System.err.println("handleXMLCommand(" + xml + ")");
    StringReader src = new StringReader("<root>" + xml.replace('\0', ' ') + "</root>");
    try {
      Element root = builder.build(src).getRootElement();
      Iterator cmds = root.getChildren().iterator();
      while (cmds.hasNext()) {
        Element command = (Element) cmds.next();
        String name = command.getName();
        if ("mode".equals(name)) {
          simpleMode = "true".equals(command.getAttribute("simple").getValue().toLowerCase());
        } else if ("timestamp".equals(name)) {
          perf(command.getAttribute("msg").getValue());
        } else if ("start".equals(name)) {
          terminalReady = true;
          buffer.update[0] = true;
          redraw();
        }
      }
    } catch (JDOMException e) {
      System.err.println("error reading command: " + e);
    }

  }

  // placeholder ...
  private Element terminal = new Element("terminal");
  private XMLOutputter xmlOutputter = new XMLOutputter();

  public void redraw() {
    if (debug > 0)
      System.err.println("FlashTerminal: redraw()");

    if (terminalReady && writer != null) {
      xmlOutputter.setNewlines(true);
      try {
        // remove children from terminal
        terminal.removeChildren();
        if (simpleMode) {
          xmlOutputter.output(redrawSimpleTerminal(terminal), writer);
          xmlOutputter.output(redrawSimpleTerminal(terminal), System.err);
        } else {
          xmlOutputter.output(redrawFullTerminal(terminal), writer);
        }
        writer.write(0);
        writer.flush();
        // perf("PERF: sent");
        if (debug > 0) System.err.println("FlashTerminal: flushed data ...");
      } catch (IOException e) {
        System.err.println("FlashTerminal: error writing to client: " + e);
        writer = null;
      }
    }
  }

  private int checkPoint = 0;

  private Element redrawSimpleTerminal(Element terminal) {
    terminal.setAttribute("simple", "true");

    System.err.println("checkPoint: "+checkPoint);
    if(checkPoint > 0) checkPoint -= 1;
    // start where we last time stopped ...
    while (checkPoint < buffer.bufSize) {
      terminal.addContent(redrawLine(0, checkPoint));
      checkPoint++;
    }

    buffer.update[0] = false;
    return terminal;
  }

  private Element redrawFullTerminal(Element terminal) {
    // cycle through buffer and create terminal update ...
    for (int l = 0; l < buffer.height; l++) {
      if (!buffer.update[0] && !buffer.update[l + 1]) continue;
      buffer.update[l + 1] = false;
      terminal.addContent(redrawLine(l, buffer.windowBase));
    }
    buffer.update[0] = false;
    return terminal;
  }

  private Element redrawLine(int l, int base) {
    Element line = new Element("line");
    line.setAttribute("row", "" + l);

    // determine the maximum of characters we can print in one go
    for (int c = 0; c < buffer.width; c++) {
      int addr = 0;
      int currAttr = buffer.charAttributes[base + l][c];

      while ((c + addr < buffer.width) &&
              ((buffer.charArray[base + l][c + addr] < ' ') ||
              (buffer.charAttributes[base + l][c + addr] == currAttr))) {
        if (buffer.charArray[base + l][c + addr] < ' ') {
          buffer.charArray[base + l][c + addr] = ' ';
          buffer.charAttributes[base + l][c + addr] = 0;
          continue;
        }
        addr++;
      }

      if (addr > 0) {
        Text text = new Text(new String(buffer.charArray[base + l], c, addr));
        Element chunk = null;
        if ((currAttr & 0xfff) != 0) {
          if ((currAttr & VDUBuffer.BOLD) != 0)
            chunk = addChunk(new Element("B"), chunk, text);
          if ((currAttr & VDUBuffer.UNDERLINE) != 0)
            chunk = addChunk(new Element("U"), chunk, text);
          if ((currAttr & VDUBuffer.INVERT) != 0)
            chunk = addChunk(new Element("I"), chunk, text);
          if ((currAttr & buffer.COLOR_FG) != 0) {
            String fg = color[((currAttr & buffer.COLOR_FG) >> 4) - 1];
            Element font = new Element("FONT").setAttribute("COLOR", fg);
            chunk = addChunk(font, chunk, text);
          }
          /*
          if ((currAttr & buffer.COLOR_BG) != 0) {
            Color bg = color[((currAttr & buffer.COLOR_BG) >> 8) - 1];
          }
          */
        }
        if (chunk == null) {
          line.addContent(text);
        } else {
          line.addContent(chunk);
        }
      }
      c += addr - 1;
    }
    return line;
  }

  private Element addChunk(Element el, Element chunk, Text text) {
    if (chunk == null)
      return el.addContent(text);
    else
      return el.addContent(chunk);
  }

  public void setVDUBuffer(VDUBuffer buffer) {
    this.buffer = buffer;
    if (simpleMode) {
      this.buffer.setCursorPosition(0, 23);
    }
    this.buffer.setDisplay(this);
    this.buffer.update[0] = true;
  }

  public VDUBuffer getVDUBuffer() {
    return buffer;
  }
}
