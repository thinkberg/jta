/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
 *
 * Please visit http://javassh.org/ for updates and contact.
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

package de.mud.ssh;

import de.mud.telnet.ScriptHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.Vector;
import java.util.Properties;

import java.awt.Dimension;

/**
 * The telnet ssh is a sample class for how to use the SSH protocol
 * handler of the JTA source package. To write a program using the wrapper
 * you may use the following piece of code as an example:
 * <PRE>
 *   SshWrapper telnet = new SshWrapper();
 *   try {
 *     ssh.connect(args[0], 23);
 *     ssh.login("user", "password");
 *     ssh.setPrompt("user@host");
 *     ssh.waitfor("Terminal type?");
 *     ssh.send("dumb");
 *     System.out.println(ssh.send("ls -l"));
 *   } catch(java.io.IOException e) {
 *     e.printStackTrace();
 *   }
 * </PRE>
 * Please keep in mind that the password is visible for anyone who can
 * download the class file. So use this only for public accounts or if
 * you are absolutely sure nobody can see the file.
 * <P>
 * <B>Maintainer:</B>Marcus Meissner
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class SshWrapper {
  protected SshIO handler;
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

  public SshWrapper() {
    handler = new SshIO() {
      /** get the current terminal type */
      public String getTerminalType() {
        return "vt320";
      }
      /** get the current window size */
      public Dimension getWindowSize() {
        return new Dimension(80,25);
      }
      /** notify about local echo */
      public void setLocalEcho(boolean echo) {
	/* EMPTY */
      }
      /** write data to our back end */
      public void write(byte[] b) throws IOException {
        out.write(b);
      }
    };
  }

  /** Connect the socket and open the connection. */
  public void connect(String host, int port) throws IOException {
    if(debug>0) System.err.println("SSHWrapper: connect("+host+","+port+")");
    try {
      socket = new java.net.Socket(host, port);
      in = socket.getInputStream();
      out = socket.getOutputStream();
    } catch(Exception e) {
      System.err.println("SshWrapper: "+e);
      disconnect();
      throw ((IOException)e);
    }
  }  
 
  /** Disconnect the socket and close the connection. */
  public void disconnect() throws IOException {
    if(debug>0) System.err.println("SshWrapper: disconnect()");
    if (socket != null)
	socket.close();
  }

  /**
   * Login into remote host. This is a convenience method and only
   * works if the prompts are "login:" and "Password:".
   * @param user the user name 
   * @param pwd the password
   */
  public void login(String user, String pwd) throws IOException {
    handler.setLogin(user);
    handler.setPassword(pwd);
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
    byte arr[]; 
    arr = (cmd+"\n").getBytes();
    // no write until authorization is done
    for (int i=0;i<arr.length;i++) {
        switch (arr[i]) {
        case 10: /* \n -> \r */
                arr[i] = 13;
                break;
        }
    }
    handler.sendData(new String(arr));
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

  public String waitfor( String[] searchElements ) throws IOException {
    ScriptHandler[] handlers = new ScriptHandler[searchElements.length];
    for ( int i = 0; i < searchElements.length; i++ ) {
      // initialize the handlers
      handlers[i] = new ScriptHandler();
      handlers[i].setup( searchElements[i] );
    }

    byte[] b = new byte[256];
    int n = 0;
    StringBuffer ret = new StringBuffer();
    String current;

    while(n >= 0) {
      n = read(b);
      if(n > 0) {
	current = new String( b, 0, n );
	if (debug > 0)
	  System.err.print( current );
	ret.append( current );
	for ( int i = 0; i < handlers.length ; i++ ) {
	  if ( handlers[i].match( b, n ) ) {
	    return ret.toString();
	  } // if
	} // for
      } // if
    } // while
    return null; // should never happen
  }

  public String waitfor(String match) throws IOException {
    String[] matches = new String[1];

    matches[0] = match;
    return waitfor(matches);
  }

  /** Buffer for SSH input */
  private byte[] buffer;
  /** Position in SSH input buffer */
  private int pos;
  
  /**
   * Read data from the backend and decrypt it. This is a buffering read
   * as the encrypted information is usually smaller than its decrypted
   * pendant. So it will not read from the backend as long as there is
   * data in the buffer.
   * @param b the buffer where to read the decrypted data in
   * @return the amount of bytes actually read.
   */
  public int read(byte[] b) throws IOException {
    // Empty the buffer before we do anything else
    if(buffer != null) {
      int amount = ((buffer.length - pos) <= b.length) ? 
                      buffer.length - pos : b.length;
      System.arraycopy(buffer, pos, b, 0, amount);
      if(pos + amount < buffer.length) {
        pos += amount;
      } else 
        buffer = null;
      return amount;
    }
 
    // now that the buffer is empty let's read more data and decrypt it
    int n = in.read(b);
    if(n > 0) {
      byte[] tmp = new byte[n];
      System.arraycopy(b, 0, tmp, 0, n);
      pos = 0;
      buffer = handler.handleSSH(tmp);
      if(debug > 0 && buffer != null && buffer.length > 0)
        System.err.println("ssh: "+buffer);

      if(buffer != null && buffer.length > 0) {
        if(debug > 0) 
	  System.err.println("ssh: incoming="+n+" now="+buffer.length);
	int amount = buffer.length <= b.length ?  buffer.length : b.length;
        System.arraycopy(buffer, 0, b, 0, amount);
	pos = n = amount;
	if(amount == buffer.length) {
	  buffer = null;
	  pos = 0;
	}
      } else
        return 0;
    }
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
