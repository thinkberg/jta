/******************************************************************************
 *
 * Copyright (c) 1998,99 by Mindbright Technology AB, Stockholm, Sweden.
 *                 www.mindbright.se, info@mindbright.se
 *
 *****************************************************************************
 * $Author$
 * $Date$
 * $Name$
 *****************************************************************************/
/*
 * !!! Author's comment: See DES.java for additional copyright-info on DES
 */
package de.mud.ssh;

import java.math.BigInteger;

public final class DES3 extends Cipher {
  DES des1 = new DES();
  DES des2 = new DES();
  DES des3 = new DES();

  public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
    des1.encrypt(src, srcOff, dest, destOff, len);
    des2.decrypt(dest, destOff, dest, destOff, len);
    des3.encrypt(dest, destOff, dest, destOff, len);
  }

  public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
    des3.decrypt(src, srcOff, dest, destOff, len);
    des2.encrypt(dest, destOff, dest, destOff, len);
    des1.decrypt(dest, destOff, dest, destOff, len);
  }

  public void setKey(byte[] key) {
    byte[] subKey = new byte[8];
    des1.setKey(key);
    System.arraycopy(key, 8, subKey, 0, 8);
    des2.setKey(subKey);
    System.arraycopy(key, 16, subKey, 0, 8);
    des3.setKey(subKey);
  }

  /* !!! DEBUG
  public static void main(String[] argv) {
    byte[] key = { 
      (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78,
      (byte)0x87, (byte)0x65, (byte)0x43, (byte)0x21,
      (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77,
      (byte)0x87, (byte)0x65, (byte)0x43, (byte)0x21,
      (byte)0x87, (byte)0x65, (byte)0x43, (byte)0x21,
      (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78,
    };

    byte[] txt = {
      (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33,
      (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77,
      (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33,
      (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77,
      (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33,
      (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77 
    };

    byte[] enc;
    byte[] dec;

    System.out.println("key: " + printHex(key));
    System.out.println("txt: " + printHex(txt));

    DES3 cipher = new DES3();
    cipher.setKey(key);

    enc = cipher.encrypt(txt);
    System.out.println("enc: " + printHex(enc));

    cipher = new DES3();
    cipher.setKey(key);

    dec = cipher.decrypt(enc);

    System.out.println("dec: " + printHex(dec));
  }

  static String printHex(byte[] buf) {
    byte[] out = new byte[buf.length + 1];
    out[0] = 0;
    System.arraycopy(buf, 0, out, 1, buf.length);
    BigInteger big = new BigInteger(out);
    return big.toString(16);
  }
  static String printHex(int i) {
    BigInteger b = BigInteger.valueOf((long)i + 0x100000000L);
    BigInteger c = BigInteger.valueOf(0x100000000L);
    if(b.compareTo(c) != -1)
      b = b.subtract(c);
    return b.toString(16);
  }

    */

}
