/*
 *
 * Created       : 2000 Jan 30 (Sun) 20:35:41 by Harold Carr.
 * Last Modified : 2000 Jan 31 (Mon) 03:59:34 by Harold Carr.
 */

package hc.net;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import de.mud.telnet.TelnetWrapper;

public class TelnetClient {
  public static void main(String[] av) {
    String host = "localhost";
    int port = 23;
    switch (av.length) {
      case 2:
        port = Integer.parseInt(av[1]);
      case 1:
        host = av[0];
      case 0:
        break;
      default :
        System.exit(-1);
    }
    new TelnetClient(System.in, System.out, host, port);
  }

  public TelnetClient(InputStream in, PrintStream out,
                      String host, int port) {
    TelnetWrapper telnetWrapper = new TelnetWrapper();
    try {
      telnetWrapper.connect(host, port);
    } catch (IOException e) {
      System.out.println("TelnetClient: Got exception during connect: " + e);
      e.printStackTrace();
    }
    createAndStartReader(telnetWrapper, out);
    byte[] buf = new byte[256];
    int n = 0;
    try {
      while (n >= 0) {
        n = in.read(buf);
        if (n > 0) {
          byte[] sendBuf = new byte[n];
          System.arraycopy(buf, 0, sendBuf, 0, n);
          // Must be transpose (not send) or connect is closed.
          telnetWrapper.getHandler().transpose(sendBuf);
        }
      }
    } catch (IOException e) {
      System.out.println("TelnetClient: Got exception in read/write loop: " + e);
      e.printStackTrace();
      return;
    } finally {
      try {
        telnetWrapper.disconnect();
      } catch (IOException e) {
        System.out.println("TelnetClient: got exception in disconnect: " + e);
        e.printStackTrace();
      }
    }
  }

  class ReaderThread extends Thread {
    TelnetWrapper telnetWrapper;
    PrintStream out;

    ReaderThread(TelnetWrapper telnetWrapper, PrintStream out) {
      super("TelnetReaderThread");
      this.telnetWrapper = telnetWrapper;
      this.out = out;
    }

    public void run() {
      System.out.println("thread-starting");
      byte[] buf = new byte[256];
      int n = 0;
      while (n >= 0) {
        try {
          n = telnetWrapper.read(buf);
          if (n > 0) {
            out.print(new String(buf, 0, n));
          }
        } catch (IOException e) {
          System.out.println("ReaderThread.run: got exception in read/write loop: " + e);
          e.printStackTrace();
          return;
        }
      }
    }
  }

  public Thread createAndStartReader(TelnetWrapper telnetWrapper,
                                     PrintStream out) {
    ReaderThread readerThread = new ReaderThread(telnetWrapper, out);
    readerThread.start();
    return readerThread;
  }
}

// End of file.
