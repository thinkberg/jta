/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
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
 */
package de.mud.ssh;

import java.math.BigInteger;

/**
 * @author Marcus Meissner
 * @version $Id$
 */
public class SshCrypto {
  private Cipher sndCipher,rcvCipher;

  public SshCrypto(String type, final byte[] key) {
    sndCipher = Cipher.getInstance(type);
    rcvCipher = Cipher.getInstance(type);

    // must be async for RC4. But we currently don't.

    sndCipher.setKey(key);
    rcvCipher.setKey(key);
  }

  public byte[] encrypt(byte[] block) {
    return sndCipher.encrypt(block);
  }

  public byte[] decrypt(byte[] block) {
    return rcvCipher.decrypt(block);
  };

  //-------------------------------------------------------------------------

  static public byte[] encrypteRSAPkcs1Twice(byte[] clearData,
                                             byte
    [] server_key_public_exponent,
                                             byte[] server_key_public_modulus,
                                             byte[] host_key_public_exponent,
                                             byte[] host_key_public_modulus) {

    // At each encryption step, a multiple-precision integer is constructed
    //
    // the integer is interpreted as a sequence of bytes, msb first;
    // the number of bytes is the number of bytes needed to represent the modulus.
    //
    // cf PKCS #1: RSA Encryption Standard.  Available for anonymous ftp at ftp.rsa.com.
    //  The sequence of byte is as follows:
    // The most significant byte is zero.
    // The next byte contains the value 2 (stands for public-key encrypted data)
    // Then, there are non zero random bytes to fill any unused space
    // a zero byte,
    // and the data to be encrypted


    byte[] EncryptionBlock;	//what will be encrypted

    int offset = 0;
    EncryptionBlock = new byte[server_key_public_modulus.length];
    EncryptionBlock[0] = 0;
    EncryptionBlock[1] = 2;
    offset = 2;
    for (int i = 2; i < (EncryptionBlock.length - clearData.length - 1); i++)
      EncryptionBlock[offset++] = SshMisc.getNotZeroRandomByte();
    EncryptionBlock[offset++] = 0;
    for (int i = 0; i < clearData.length; i++)
      EncryptionBlock[offset++] = clearData[i];

    //EncryptionBlock can be encrypted now !
    BigInteger m, e, message;
    byte[] messageByte;


    m = new BigInteger(1, server_key_public_modulus);
    e = new BigInteger(1, server_key_public_exponent);
    message = new BigInteger(1, EncryptionBlock);
    //      byte[] messageByteOld1 = message.toByteArray();

    message = message.modPow(e, m);	//RSA Encryption !!

    byte[] messageByteTemp = message.toByteArray();	//messageByte holds the encypted data.
    //there should be no zeroes a the begining but we have to fix it (JDK bug !!)
    messageByte = new byte[server_key_public_modulus.length];
    int tempOffset = 0;
    while (messageByteTemp[tempOffset] == 0)
      tempOffset++;
    for (int i = messageByte.length - messageByteTemp.length + tempOffset;
         i < messageByte.length; i++)
      messageByte[i] = messageByteTemp[tempOffset++];


    // we can't check that the crypted message is OK : no way to decrypt :-(

    //according to the ssh source  !!!!! Not well explained in the protocol!!!
    clearData = messageByte;

    //SECOND ROUND !!

    offset = 0;
    EncryptionBlock = new byte[host_key_public_modulus.length];
    EncryptionBlock[0] = 0;
    EncryptionBlock[1] = 2;

    offset = 2;
    for (int i = 2; i < (EncryptionBlock.length - clearData.length - 1); i++)
      EncryptionBlock[offset++] = SshMisc.getNotZeroRandomByte();	//random !=0
    EncryptionBlock[offset++] = 0;
    for (int i = 0; i < clearData.length; i++)
      EncryptionBlock[offset++] = clearData[i];

    //EncryptionBlock can be encrypted now !

    m = new BigInteger(1, host_key_public_modulus);
    e = new BigInteger(1, host_key_public_exponent);
    message = new BigInteger(1, EncryptionBlock);

    message = message.modPow(e, m);

    messageByteTemp = message.toByteArray();	//messageByte holds the encypted data.
    //there should be no zeroes a the begining but we have to fix it (JDK bug !!)
    messageByte = new byte[host_key_public_modulus.length];
    tempOffset = 0;
    while (messageByteTemp[tempOffset] == 0)
      tempOffset++;
    for (int i = messageByte.length - messageByteTemp.length + tempOffset;
         i < messageByte.length; i++)
      messageByte[i] = messageByteTemp[tempOffset++];

    //Second encrypted key : encrypted_session_key //mp-int
    byte[] encrypted_session_key = new byte[host_key_public_modulus.length + 2];	//encrypted_session_key is a mp-int !!!

    //the lengh of the mp-int.

    encrypted_session_key[1] = (byte) ((8 * host_key_public_modulus.length) & 0xff);

    encrypted_session_key[0] = (byte) (((8 * host_key_public_modulus.length) >> 8) & 0xff);
    //the mp-int
    for (int i = 0; i < host_key_public_modulus.length; i++)
      encrypted_session_key[i + 2] = messageByte[i];
    return encrypted_session_key;
  };
}
