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

public class FlashTerminal implements VDUDisplay {
  private final static int debug = 0;
  private VDUBuffer buffer;

  public void redraw() {
    if (debug > 0)
      System.err.println("redraw()");

    for (int l = 0; l < buffer.height; l++) {
      if (!buffer.update[0] && !buffer.update[l + 1]) continue;
      buffer.update[l + 1] = false;
      if (debug > 2) System.err.println("redraw(): line " + l);
      // determine the maximum of characters we can print in one go

      for (int c = 0; c < buffer.width; c++) {
        int addr = 0;
        int currAttr = buffer.charAttributes[buffer.windowBase + l][c];

        while ((c + addr < buffer.width) &&
                ((buffer.charArray[buffer.windowBase + l][c + addr] < ' ') ||
                (buffer.charAttributes[buffer.windowBase + l][c + addr] == currAttr))) {
          if (buffer.charArray[buffer.windowBase + l][c + addr] < ' ') {
            buffer.charArray[buffer.windowBase + l][c + addr] = ' ';
            buffer.charAttributes[buffer.windowBase + l][c + addr] = 0;
            continue;
          }
          addr++;
        }

        if (addr > 0) {
          System.out.println("<draw columns=\"" + c + "\" row=\"" + l + "\">" +
                             new String(buffer.charArray[buffer.windowBase + l], c, addr) +
                             "</draw>");
        }

        c += addr - 1;
      }
    }
    buffer.update[0] = false;
  }

  public void setVDUBuffer(VDUBuffer buffer) {
    this.buffer = buffer;
    buffer.setDisplay(this);
  }

  public VDUBuffer getVDUBuffer() {
    return buffer;
  }
}
