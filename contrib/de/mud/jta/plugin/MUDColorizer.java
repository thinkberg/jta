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

import de.mud.jta.FilterPlugin;
import de.mud.jta.Plugin;
import de.mud.jta.PluginBus;
import de.mud.jta.PluginConfig;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.EndOfRecordListener;
import gnu.regexp.RE;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Some little hack for colors and prompts.
 * We are using GNU, so we should release this under the GPL :)
 * <ul>
 * <li>needs gnu.regexp package (approx. 22 kB)
 * <li>handles prompt with EOR (maybe buggy, but testet with mglib 3.2.6)
 * <li>colorizes single lines using regular expressions
 * </ul>
 * @author Thomas Kriegelstein
 */
public class MUDColorizer extends Plugin
        implements FilterPlugin, EndOfRecordListener, ConfigurationListener {

  public static String BLACK = "[30m";
  public static String RED = "[31m";
  public static String BRED = "[1;31m";
  public static String GREEN = "[32m";
  public static String BGREEN = "[1;32m";
  public static String YELLOW = "[33m";
  public static String BYELLOW = "[1;33m";
  public static String BLUE = "[34m";
  public static String BBLUE = "[1;34m";
  public static String PINK = "[35m";
  public static String BPINK = "[1;35m";
  public static String CYAN = "[36m";
  public static String BCYAN = "[1;36m";
  public static String WHITE = "[37m";
  public static String BWHITE = "[1;37m";
  public static String NORMAL = "[0m";
  public static String BOLD = "[1m";

  /*  Prompthandling:
   *  if we do have a prompt, in every read of a new line, write a
   *  Clearline (ie. \r\e[K), then write the text and then
   *  rewrite the prompt after the last \n
   */

  private Object[] exps = null;

  public MUDColorizer(PluginBus bus, final String id) {
    super(bus, id);
    bus.registerPluginListener(this);
  }

  public void setConfiguration(PluginConfig cfg) {
    String tmp;
    if ((tmp = cfg.getProperty("MUDColorizer", id, "regexpSet")) != null) {
      Properties regexpSet = new Properties();

      try {
        regexpSet.load(getClass().getResourceAsStream(tmp));
      } catch (Exception e) {
        try {
          regexpSet.load(new URL(tmp).openStream());
        } catch (Exception ue) {
          error("cannot find regexpSet: " + tmp);
          error("resource access failed: " + e);
          error("URL access failed: " + ue);
          regexpSet = null;
        }
      }

      if (regexpSet != null && !regexpSet.isEmpty()) {
        exps = new Object[regexpSet.size() * 2];
        Hashtable colors = new Hashtable();
        colors.put("BLACK", BLACK);
        colors.put("RED", RED);
        colors.put("BRED", BRED);
        colors.put("GREEN", GREEN);
        colors.put("BGREEN", BGREEN);
        colors.put("YELLOW", YELLOW);
        colors.put("BYELLOW", BYELLOW);
        colors.put("BLUE", BLUE);
        colors.put("BBLUE", BBLUE);
        colors.put("PINK", PINK);
        colors.put("BPINK", BPINK);
        colors.put("CYAN", CYAN);
        colors.put("BCYAN", BCYAN);
        colors.put("WHITE", WHITE);
        colors.put("BWHITE", BWHITE);
        colors.put("NORMAL", NORMAL);
        colors.put("BOLD", BOLD);

        Enumeration names = regexpSet.propertyNames();
        int ex = 0;
        while (names.hasMoreElements()) {
          String exp = (String) names.nextElement();
          RE re = null;
          try {
            re = new RE(exp);
          } catch (Exception e) {
            System.err.println("Something wrong with regexp: " +
                               ex + "\t" + exp);
            System.err.println(e);
          }
          exps[ex++] = re;
          exps[ex++] = colors.get(regexpSet.get(exp));
          System.out.println("MUDColorizer: loaded: " + exp + " with " + regexpSet.get(exp));
        }
      }
    }
  }

  FilterPlugin source;

  public void setFilterSource(FilterPlugin source) {
    this.source = source;
  }

  public FilterPlugin getFilterSource() {
    return source;
  }

  public void EndOfRecord() {
    readprompt = true;
  }

  private byte[] transpose(byte[] buf) {
    byte[] nbuf;
    int nbufptr = 0;
    nbuf = new byte[8192];

    /* Prompthandling I */
    if (promptwritten && prompt != null && prompt.length > 0) {
      // "unwrite"
      nbuf[nbufptr++] = (byte) '\r';
      nbuf[nbufptr++] = 27;
      nbuf[nbufptr++] = (byte) '[';
      nbuf[nbufptr++] = (byte) 'K';
      promptwritten = false;
    }
    if (readprompt) {
      int index;
      for (index = buf.length - 1; index >= 0; index--)
        if (buf[index] == '\n') break;
      index++;
      prompt = new byte[buf.length - index];
      System.arraycopy(buf, index, prompt, 0, buf.length - index);
      readprompt = false;
      writeprompt = true;
      promptwritten = false;
      promptread = true;
      // System.out.println("Neues Prompt: $"+new String(prompt)+"$");
    }
    /* /Prompthandling I */

    /* Colorhandling should be done herein
     * Problem:  Strings aren�t allways transposed completely
     *           sometimes a \n is in the next transpose buffer
     * Solution: Buffer lines outside like read does
     */
    if (promptwritten) {
      lp = 0;
      line[0] = 0;
    }

    for (int i = 0; i < buf.length; i++, lp++) {
      // nbuf[nbufptr++] = buf[i];
      line[lp] = buf[i];
      if (line[lp] == '\n') {
        String l = new String(line, 0, lp + 1);
        boolean colored = false;
        boolean useexp = (exps != null);
        for (int ex = 0; !colored && useexp && ex < exps.length; ex += 2) {
          RE exp = (RE) exps[ex];
          if (null != exp.getMatch(l)) {
            byte[] color = (byte[]) ((String) exps[ex + 1]).getBytes();
            System.arraycopy(color, 0, nbuf, nbufptr, color.length);
            nbufptr += color.length;
            System.arraycopy(line, 0, nbuf, nbufptr, lp + 1);
            nbufptr += lp + 1;
            byte[] normal = NORMAL.getBytes();
            System.arraycopy(normal, 0, nbuf, nbufptr, normal.length);
            nbufptr += normal.length;
            colored = true;
          }
        }
        if (!colored) {
          System.arraycopy(line, 0, nbuf, nbufptr, lp + 1);
          nbufptr += lp + 1;
        }
        colored = false;
        lp = -1;
        line[0] = 0; // gets overwritten soon;
      }
    }
    if (promptread) {
      lp = 0;
      line[0] = 0;
      promptread = false;
    }
    /* /Colorhandling */

    /* Prompthandling II */
    if (buf[buf.length - 1] == '\n') writeprompt = true;
    if (buf[buf.length - 1] == '\r') writeprompt = true;
    if (writeprompt && prompt != null && prompt.length > 0) {
      // "rewrite"
      nbuf[nbufptr++] = (byte) '\r';
      nbuf[nbufptr++] = 27;
      nbuf[nbufptr++] = (byte) '[';
      nbuf[nbufptr++] = (byte) 'K';
      System.arraycopy(prompt, 0, nbuf, nbufptr, prompt.length);
      nbufptr += prompt.length;
      promptwritten = true;
      writeprompt = false;
    }
    /* /Promphandling II */

    byte[] xbuf = new byte[nbufptr];
    System.arraycopy(nbuf, 0, xbuf, 0, nbufptr);
    return xbuf;
  }

  // einzufaerbende zeile
  private int lp = 0;
  private byte[] line = new byte[8192];
  // prompt handeln
  private boolean readprompt = false;
  private boolean promptread = false;
  private boolean writeprompt = false;
  private boolean promptwritten = false;
  private byte[] prompt = null;
  // bufferoverflows handeln
  private byte[] buffer = null;
  private int pos = 0;

  public int read(byte[] b) throws IOException {
    // empty the buffer before reading more data
    if (buffer != null) {
      int amount = (buffer.length - pos) <= b.length ?
              buffer.length - pos : b.length;
      System.arraycopy(buffer, pos, b, 0, amount);
      if (pos + amount < buffer.length) {
        pos += amount;
      } else {
        buffer = null;
        pos = 0;
      }
      return amount;
    }

    // now we are sure the buffer is empty and read on
    int n = source.read(b);
    if (n > 0) {
      byte[] tmp = new byte[n];
      System.arraycopy(b, 0, tmp, 0, n);
      buffer = transpose(tmp);
      if (buffer != null && buffer.length > 0) {
        int amount = buffer.length <= b.length ? buffer.length : b.length;
        System.arraycopy(buffer, 0, b, 0, amount);
        pos = n = amount;
        if (amount == buffer.length) {
          buffer = null;
          pos = 0;
        }
      } else
        return 0;
    }
    return n;
  }

  public void write(byte[] b) throws IOException {
    if (b[b.length - 1] == '\n') {
      writeprompt = true;
      promptwritten = false;
    }
    source.write(b);
  }
}
