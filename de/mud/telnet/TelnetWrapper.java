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

package de.mud.telnet;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.Vector;
import java.util.Properties;

import java.awt.Dimension;

/**
 * The telnet wrapper is a sample class for how to use the telnet protocol
 * handler of the JTA source package. To write a program using the wrapper
 * you may use the following piece of code as an example:
 * <PRE>
 *   TelnetWrapper telnet = new TelnetWrapper();
 *   try {
 *     telnet.connect(args[0], 23);
 *     telnet.login("user", "password");
 *     telnet.setPrompt("user@host");
 *     telnet.waitfor("Terminal type?");
 *     telnet.send("dumb");
 *     System.out.println(telnet.send("ls -l"));
 *   } catch(java.io.IOException e) {
 *     e.printStackTrace();
 *   }
 * </PRE>
 * Please keep in mind that the password is visible for anyone who can
 * download the class file. So use this only for public accounts or if
 * you are absolutely sure nobody can see the file.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meißner
 */
public class TelnetWrapper extends TelnetProtocolHandler {

  /** debugging level */
  private final static int debug = 0;

  protected ScriptHandler scriptHandler = new ScriptHandler();
  private Thread reader;

  protected InputStream in;
  protected OutputStream out;
  protected Socket socket;
  protected String host;
  protected int port = 23;
  protected Vector script = new Vector();

  /** Connect the socket and open the connection. */
  public void connect(String host, int port) throws IOException {
    if(debug>0) System.err.println("TelnetWrapper: connect("+host+","+port+")");
    try {
      socket = new java.net.Socket(host, port);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      reset();
    } catch(Exception e) {
      System.err.println("TelnetWrapper: "+e);
      disconnect();
      throw ((IOException)e);
    }
  }  
 
  /** Disconnect the socket and close the connection. */
  public void disconnect() throws IOException {
    if(debug>0) System.err.println("TelnetWrapper: disconnect()");
    if (socket != null)
	socket.close();
  }

  /** sent on IAC EOR (prompt terminator for remote access systems). */
  public void notifyEndOfRecord() {
  }

  /**
   * Login into remote host. This is a convenience method and only
   * works if the prompts are "login:" and "Password:".
   * @param user the user name 
   * @param pwd the password
   */
  public void login(String user, String pwd) throws IOException {
    waitfor("login:");		// throw output away
    send(user);
    waitfor("Password:");	// throw output away
    send(pwd);
  }

  /**
   * Set the prompt for the send() method.
   */
  private String prompt = null;
  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  /**
   * Send a command to the remote host. A newline is appended and if
   * a prompt is set it will return the resulting data until the prompt
   * is encountered.
   * @param cmd the command
   * @return output of the command or null if no prompt is set
   */
  public String send(String cmd) throws IOException {
    write((cmd+"\n").getBytes());
    if(prompt != null)
      return waitfor(prompt);
    return null;
  }

  /**
   * Wait for a string to come from the remote host and return all
   * that characters that are received until that happens (including
   * the string being waited for).
   *
   * @param match the string to look for
   * @return skipped characters
   */
  public String waitfor(String match) throws IOException {
    scriptHandler.setup(match);
    byte[] b = new byte[256];
    int n = 0;
    String ret = "";
    while(n >= 0) {
      n = read(b);
      if(n > 0) {
        if(debug > 0) System.err.print(new String(b, 0, n));
        ret += new String(b, 0, n);
        if(scriptHandler.match(b, n))
        return ret;
      }
    }
    return null; // should never happen
  }

  /**
   * Read data from the socket and use telnet negotiation before returning
   * the data read.
   * @param b the input buffer to read in
   * @return the amount of bytes read
   */
  public int read(byte[] b) throws IOException {
    int n = in.read(b);
    if(n > 0) n = negotiate(b, n);
    return n;
  }

  /**
   * Write data to the socket.
   * @param b the buffer to be written
   */
  public void write(byte[] b) throws IOException {
    out.write(b);
  }

  public String getTerminalType() {
    return "dumb";
  }

  public Dimension getWindowSize() {
    return new Dimension(80,24);
  }

  public void setLocalEcho(boolean echo) {
    if(debug > 0)
      System.err.println("local echo "+(echo ? "on" : "off"));
  }
}
