/**
 * SshCrypto
 * --
 * 
 * this class implement 
 * - IDEA in CFB mode
 * - RSA PKCS #1 
 * 
 * This file is part of "The Java Ssh Applet".
 */

package de.mud.ssh;

import cryptix.crypt.IDEA;
import java.math.BigInteger; //used to implement RSA : cryptics is crap ..



class SshCrypto {


	private IDEA blockCipherIDEA;

	private byte[]	IDEA_Vector_Receive = new byte[8], 
					IDEA_Vector_Send = new byte[8];






	public SshCrypto(byte[] key){
			
		blockCipherIDEA = new IDEA(key); //This creates an IDEA block cipher instance, with key data taken from a 16-byte array. 
	}



	//-------------------------------------------------------------------------------

	
	public void encrypt(byte[] block) { //block is modified

		int numberOfRound = (block.length / 8);
		int offset = 0;
		byte[] blockOfHeight = new byte[8];

		for (int round=0; round < numberOfRound; round++) {
			int offsetBis = offset;
			for (int i=0;i<8;i++) blockOfHeight[i] = block[offset++];
			blockCipherIDEA.encrypt(IDEA_Vector_Send);	
			for (int i=0; i<8; i++) blockOfHeight[i] =(byte) ((blockOfHeight[i] & 0xff)  ^ (IDEA_Vector_Send[i] & 0xff) & 0xff);
			for (int i=0; i<8; i++) IDEA_Vector_Send[i] = blockOfHeight[i];
			for (int i=0;i<8;i++) block[offsetBis++] = blockOfHeight[i];
		} //for (int round=0;
	}





	//-------------------------------------------------------------------------------





	//	 SSH_CIPHER_IDEA
	//	The key is taken from the first 16 bytes of the session key.
	//	IDEA [IDEA] is used in CFB mode.  The initialization vector is
	//	initialized to all zeroes.
	//  CFB mode: a ciphertext block is obtained by encrypting the previous 
	//  ciphertext block, and xoring the resulting value with the plaintext. 
	
	public void decrypt(byte[] block) { //block is modified (decrypted)
	
		int numberOfRound = (block.length / 8);
		int offset = 0;
		byte[] blockOfHeight = new byte[8];
		for (int round=0; round < numberOfRound; round++) { //numberOfRound is (packet_length/8 + 1) so >=1
			int offsetBis = offset;
			blockCipherIDEA.encrypt(IDEA_Vector_Receive);
			byte [] next_IDEA_Vector = new byte[8];
			for (int i=0;i<8;i++) blockOfHeight[i] = next_IDEA_Vector[i] = block[offset++];
			for (int i=0;i<8;i++) {
				int int1 = (IDEA_Vector_Receive[i] & 0xff);
				int int2 = (blockOfHeight[i] & 0xff);
				blockOfHeight[i] =(byte) ((int1 ^ int2) & 0xff);
			}
			for (int i=0;i<8;i++) block[offsetBis++] = blockOfHeight[i];
			IDEA_Vector_Receive = next_IDEA_Vector;
		} //for (int round=0;for (int i=0; i<8; i++) 
	}; //decrypt




	//-------------------------------------------------------------------------------
	
	static public byte[] encrypteRSAPkcs1Twice(	byte[] clearData,
												byte[] server_key_public_exponent,
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
		
		
		byte[] EncryptionBlock;		//what will be encrypted
		
		int offset = 0;
		EncryptionBlock = new byte[server_key_public_modulus.length];
		EncryptionBlock[0] = 0;
		EncryptionBlock[1] = 2;
		offset = 2;
		for(int i=2; i<(EncryptionBlock.length - clearData.length - 1); i++) 
			EncryptionBlock[offset++] = SshMisc.getNotZeroRandomByte();
		EncryptionBlock[offset++] = 0;
		for(int i=0; i<clearData.length; i++) 
			EncryptionBlock[offset++] = clearData[i];

		//EncryptionBlock can be encrypted now !
		BigInteger	m, e, message;
		byte[] messageByte;


		m =  new BigInteger(1,server_key_public_modulus);
		e = new BigInteger(1,server_key_public_exponent);
		message = new BigInteger(1,EncryptionBlock);
	//	byte[] messageByteOld1 = message.toByteArray();

		message = message.modPow(e,m);  //RSA Encryption !!
		
		byte[] messageByteTemp = message.toByteArray(); //messageByte holds the encypted data.
		//there should be no zeroes a the begining but we have to fix it (JDK bug !!)
		messageByte = new byte[server_key_public_modulus.length];
		int tempOffset = 0;
		while(messageByteTemp[tempOffset]==0) tempOffset++;
		for(int i=messageByte.length - messageByteTemp.length + tempOffset; i<messageByte.length; i++) 
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
		for(int i=2; i<(EncryptionBlock.length - clearData.length - 1); i++) 
			EncryptionBlock[offset++] = SshMisc.getNotZeroRandomByte(); //random !=0
		EncryptionBlock[offset++] = 0;
		for(int i=0; i<clearData.length; i++) 
			EncryptionBlock[offset++] = clearData[i];

		//EncryptionBlock can be encrypted now !

		m = new BigInteger(1,host_key_public_modulus);
 		e = new BigInteger(1,host_key_public_exponent);
		message = new BigInteger(1,EncryptionBlock);

		message = message.modPow(e,m);

		messageByteTemp = message.toByteArray(); //messageByte holds the encypted data.
		//there should be no zeroes a the begining but we have to fix it (JDK bug !!)
		messageByte = new byte[host_key_public_modulus.length];
		tempOffset = 0;
		while(messageByteTemp[tempOffset]==0) tempOffset++;
		for(int i=messageByte.length - messageByteTemp.length + tempOffset; i<messageByte.length; i++) 
			messageByte[i] = messageByteTemp[tempOffset++];

		//Second encrypted key : encrypted_session_key //mp-int
		byte[] encrypted_session_key = new byte[host_key_public_modulus.length+2]; //encrypted_session_key is a mp-int !!!
		//the lengh of the mp-int.
		encrypted_session_key[1] = (byte) ((8*host_key_public_modulus.length) & 0xff);
		encrypted_session_key[0] = (byte)(((8*host_key_public_modulus.length)>>8) & 0xff);
		//the mp-int
		for(int i=0; i<host_key_public_modulus.length;i++)
			encrypted_session_key[i+2] = messageByte[i];
	
		return encrypted_session_key;

		}; // encryptRSAPkcs1Twice


}	//class SshCrypto
