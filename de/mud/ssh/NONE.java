package de.mud.ssh;

public final class NONE extends Cipher {

  public void setKey(String skey) {
  }

  // Set key of this Blowfish from a bytearray

  public void setKey(byte[] key) {
  }

  public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
    System.arraycopy(src,srcOff,dest,destOff,len);
  }

  public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
    System.arraycopy(src,srcOff,dest,destOff,len);
  }
}
