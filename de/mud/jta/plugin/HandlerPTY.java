// JNI interface to slave process.
// This is a part of Shell plugin.
// If not for a static member, we'd have HandlerPTY private to Shell. XXX

// HandlerPTY is meant to be robust, in a way that you can
// instantiate it and work with it until it throws an exception,
// then forget it. A finalizer takes care of file descriptors.

package de.mud.jta.plugin;

public class HandlerPTY {
  public native int start(String cmd);	// open + fork/exec
  public native void close();
  public native int read(byte[] b);
  public native int write(byte[] b);

  private int fd;
  boolean good = false;

  static {
    // System.loadLibrary("libutil");	// forkpty on Linux lives in libutil
    System.loadLibrary("jtapty");
  }

  protected void finalize() throws Throwable {
    super.finalize();
    if(good) {
      close();
    }
  }
}
