/*
 * This file is part of "JTA - Telnet/SSH for the JAVA(tm) platform".
 *
 * (c) Matthias L. Jugel, Marcus Meißner 1996-2005. All Rights Reserved.
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
package de.mud.jta.plugin;

import de.mud.bsx.BSXDisplay;
import de.mud.jta.FilterPlugin;
import de.mud.jta.Plugin;
import de.mud.jta.PluginBus;
import de.mud.jta.VisualPlugin;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import java.io.IOException;
import java.awt.FlowLayout;

/**
 * ultrahighspeed-BSX-command-parser as Plugin for JTA 2.0.
 * Features:
 <UL>
 <LI> BSX-Commands: @RFS, @DFS, @DFO, @RQV, @SCE, @VIO, @RMO, @TMS
 <LI> faulttolerant handling of buggy BSX data -> ignoring until @RFS
 <LI> own support package: de.mud.bsx
 </UL>
 @version Java 1.0
 @author  Thomas Kriegelstein (tk4@rb.mud.de)
 */
public class BSX extends Plugin
        implements FilterPlugin, VisualPlugin {

  /** the canvas that contains the Gfx */
  protected BSXDisplay visual = new BSXDisplay();
  /** the container for this plugin */
  protected JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

  /* the BSX Commands to be understood */
  private static final byte[] RFS = "@RFS".getBytes();
  private static final byte[] SCE = "@SCE".getBytes();
  private static final byte[] VIO = "@VIO".getBytes();
  private static final byte[] DFS = "@DFS".getBytes();
  private static final byte[] DFO = "@DFO".getBytes();
  private static final byte[] RMO = "@RMO".getBytes();
  private static final byte[] TMS = "@TMS".getBytes();
  private static final byte[] RQV = "@RQV".getBytes();
  /** the BSX style version of this Parser */
  protected static String VERSION = "Java 1.0";
  /** ignoreErrors in BSX data */
  protected boolean ignoreErrors = true;

  /**
   * initialize the parser
   */
  public BSX(PluginBus bus, final String id) {
    super(bus, id);
    panel.add(visual);
    reset();
  }

  public JComponent getPluginVisual() {
    return panel;
  }

  public JMenu getPluginMenu() {
    return null;
  }

  FilterPlugin source;

  public void setFilterSource(FilterPlugin source) {
    this.source = source;
  }

  public FilterPlugin getFilterSource() {
    return source;
  }

  public int read(byte[] b) throws IOException {
    int len;
    len = source.read(b);
    len = parse(b, len);
    return len;
  }

  public void write(byte[] b) throws IOException {
    source.write(b);
  }

  private void write(String s) throws IOException {
    write(s.getBytes());
  }

  /* ********************************************************************* */
  /* "Ultrahighspeed" Statemachine for BSX Sequences                       */
  /* ********************************************************************* */
  /*                         Buffers & States                              */
  /* ********************************************************************* */
  private byte[] cmd = new byte[4];      // command                      */
  private int cmdlen = 0;                // length of command            */
  private byte[] id = new byte[64];     // identifier                   */
  private int idlen = 0;                // length of identifier         */
  private String obj = null;             // string representation of id  */
  private int[][] data = null;             // data                         */
  private byte[] hex = new byte[2];      // 00-FF integer                */
  private int hexlen = 0;                // length of integer            */
  private byte[] res = new byte[4096];   // storage for parse-result     */
  /* ********************************************************************* */
  private int polys = 0; // 0..31        number of polygons in data     */
  private int edges = 0; // 0..31        number of edges in data        */
  private int poly = 0; // 0..31        current polygon in data        */
  private int pos = 0; // 0..edges*2+1 current position in polygon    */
  private int xpos = 0; // 0..15        xpos of object                 */
  private int ypos = 0; // 0..7         ypos of object                 */
  private int state = 0; // 0..8         what to do next                */
  /* ********************************************************************* */
  /* 0 read until next '@'                                                 */
  /* 1 read command                                                        */
  /* 2 read identifier                                                     */
  /* 3 read polygoncount                                                   */
  /* 4 read edgecount                                                      */
  /* 5 read polygondata                                                    */
  /* 6 read xpos                                                           */
  /* 7 read ypos                                                           */
  /* 8 error in data -> discard until @RFS                                 */
  /* ********************************************************************* */

  /**
   * reset the parser
   */
  protected void reset() {
    cmdlen = idlen = hexlen = 0;
    data = null;
    obj = null;
    polys = edges = poly = pos = xpos = ypos = state = 0;
  }

  private void DBG(String arg) {
    System.err.println("BSX:\t" + arg);
  }

  /**
   * parse the input buffer
   @param b      input buffer byte array
   @param length count of valid bytes in buffer
   @return       new length of valid bytes in buffer
   */
  protected int parse(byte[] b, int length) throws IOException {
    int index,resindex;

    for (index = resindex = 0; index < length; index++) {
      switch (state) {
        case 0: // read until next @
          if ((char) b[index] == '@') {
            cmd[cmdlen++] = b[index];
            state = 1;
          } else {
            res[resindex++] = b[index];
          }
          break;
        case 1: // read command
          if ((char) b[index] == '@') {
            for (int i = 0; i < cmdlen; i++)
              res[resindex++] = cmd[i];
            cmdlen = 0;
            cmd[cmdlen++] = b[index];
          } else {
            cmd[cmdlen++] = b[index];
            if (cmdlen == 4) {
              if (equals(cmd, RFS)) {
                visual.refreshScene();
                reset();
              } else if (equals(cmd, RQV)) {
                write("#VER " + VERSION + "\n");
                reset();
              } else if (equals(cmd, SCE)) {
                state = 2;
              } else if (equals(cmd, VIO)) {
                state = 2;
              } else if (equals(cmd, DFO)) {
                state = 2;
              } else if (equals(cmd, RMO)) {
                state = 2;
              } else if (equals(cmd, DFS)) {
                state = 2;
              } else if (equals(cmd, TMS)) {

                byte[] temp = "\n\n\tTerminate Session!\n\n".getBytes();
                for (int i = 0; i < temp.length; i++)
                  res[resindex++] = temp[i];
                reset();
              } else {
                for (int i = 0; i < cmdlen; i++)
                  res[resindex++] = cmd[i];
                reset();
              }
            }
          }
          break;
        case 2: // read identifier
          if ((char) b[index] == '@') {
            for (int i = 0; i < cmdlen; i++)
              res[resindex++] = cmd[i];
            for (int i = 0; i < idlen; i++)
              res[resindex++] = id[i];
            cmdlen = 0;
            cmd[cmdlen++] = b[index];
            idlen = 0;
            state = 1;
          } else if ((char) b[index] != '.') {
            id[idlen++] = b[index];
          } else {
            obj = new String(id, 0, idlen);
            if (equals(cmd, SCE)) {
              String query = visual.showScene(obj);
              if (query != null)
                write(query);
              reset();
            } else if (equals(cmd, VIO)) {
              state = 6;
            } else if (equals(cmd, RMO)) {
              visual.removeObject(obj);
              reset();
            } else if (equals(cmd, DFS)) {
              state = 3;
            } else if (equals(cmd, DFO)) {
              state = 3;
            }
          }
          break;
        case 3: // read polygoncount
          hex[hexlen++] = b[index];
          if (hexlen == 2) {
            polys = hexToInt(hex);
            if (polys > 32 || polys < 0) {
              DBG("polys " + polys + "\t" + obj);
              if (ignoreErrors) {
                DBG("ignoring till @RFS");
                cmdlen = 0;
                state = 8;
              } else {
                reset();
              }
            } else {
              data = new int[polys][];
              if (polys > 0) {
                state = 4;
              } else { // Empty BSX is "00"
                if (equals(cmd, DFS)) {
                  visual.defineScene(obj,
                                     data);
                } else if (equals(cmd, DFO)) {
                  visual.defineObject(obj,
                                      data);
                }
                reset();
              }
              hexlen = 0;
            }
          }
          break;
        case 4: // read edgecount
          hex[hexlen++] = b[index];
          if (hexlen == 2) {
            edges = hexToInt(hex);
            if (edges > 32 || edges < 0) {
              DBG("edges " + edges + "\t" + obj);
              if (ignoreErrors) {
                DBG("\tignoring till @RFS");
                cmdlen = 0;
                state = 8;
              } else {
                reset();
              }
            } else {
              data[poly] = new int[1 + edges * 2];
              state = 5;
              hexlen = 0;
            }
          }
          break;
        case 5: // read polygondata
          hex[hexlen++] = b[index];
          if (hexlen == 2) {
            int c = hexToInt(hex);
            if (c < 0) {
              DBG("edge " + c + "\t" + obj);
              if (ignoreErrors) {
                DBG("\tignoring till @RFS");
                cmdlen = 0;
                state = 8;
              } else {
                reset();
              }
            } else {
              data[poly][pos] = c;
              hexlen = 0;
              pos++;
              if (pos == edges * 2 + 1) {
                poly++;
                state = 4;
                pos = 0;
                if (poly == polys) {
                  if (equals(cmd, DFS)) {
                    visual.defineScene(obj,
                                       data);
                  } else if (equals(cmd, DFO)) {
                    visual.defineObject(obj,
                                        data);
                  }
                  reset();
                }
              }
            }
          }
          break;
        case 6: // read xpos
          hex[hexlen++] = b[index];
          if (hexlen == 2) {
            xpos = hexToInt(hex);
            if (xpos > 15 || xpos < 0) {
              DBG("xpos " + xpos + "\t" + obj);
              reset();
            } else {
              state = 7;
              hexlen = 0;
            }
          }
          break;
        case 7: // read ypos
          hex[hexlen++] = b[index];
          if (hexlen == 2) {
            ypos = hexToInt(hex);
            if (ypos > 7 || ypos < 0) {
              DBG("ypos " + ypos + "\t" + obj);
              reset();
            } else {
              String query = visual.showObject(obj, xpos, ypos);
              if (query != null)
                write(query);
              reset();
            }
          }
          break;
        case 8: // error in data -> read until @RFS
          if ((char) b[index] == '@') // start of new command sequence
          {
            cmdlen = 0;
            cmd[cmdlen++] = b[index];
          } else if (cmdlen > 0) // read command sequence
          {
            cmd[cmdlen++] = b[index];
            if (cmdlen == 4) // command sequence complete
            {
              if (equals(cmd, RFS)) {
                visual.refreshScene();
                reset();
              } else // wrong command
              {
                cmdlen = 0;
              }
            }
          } else // discard this byte
          {
          }
          break;
      }
    }
    System.arraycopy(res, 0, b, 0, resindex);
    return resindex;
  }

  /**
   * compares two byte[]
   @return true if they contain the same values
   */
  protected boolean equals(byte[] a, byte[] b) {
    for (int i = 0; i < a.length && i < b.length; i++)
      if (a[i] != b[i]) return false;
    return a.length == b.length;
  }

  /**
   * computes an integer from an byte[2] containing a
   * hexadecimal representation in capitol letters (0-9,A-F)
   @return -1 on parseerror
   */
  protected int hexToInt(byte[] b) {
    int f = 0,g = 0;
    char h = 0,i = 0;

    h = (char) b[0];
    i = (char) b[1];
    if (h >= 'A' && h <= 'F')
      f = h - 'A' + 10;
    else if (h >= '0' && h <= '9')
      f = h - '0';
    else
      return -1;
    if (i >= 'A' && i <= 'F')
      g = i - 'A' + 10;
    else if (i >= '0' && i <= '9')
      g = i - '0';
    else
      return -1;
    return f * 16 + g;
  }
}

