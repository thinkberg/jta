/**
 * SshPacket
 * --
 *
 * This class provides the Ssh packet layer protocol
 *
 * This file is part of "The Java Ssh Applet".
 */

package de.mud.ssh;


//SshPacket(newData, newType)	//create the corresponding packet
				//(length + padding + packet_type + data + crc)
				//encrypts it -> encryptedBlock

//setBlock()			//block <- padding + packet_type + data +crc

//getBytes			//return packet_length + encrypted_block

//getPacketfromBytes		//-> packet_length_array and encrypted_block
				// decrypt(encrypted_block) -> decryptedBlock
				// setPacketFromDecryptedBlock
				// checkCrc


//setPacketFromDecryptedBlock	//  decryptedBlock -> padding + packet_type + data +crc

//getType

//getData


import java.io.IOException;

abstract class SshPacket {
  public SshPacket() { /* nothing */ }
  abstract public byte[] getBytes() throws IOException;
  abstract public byte[] getData() throws IOException ;
  abstract public byte getType() throws IOException ;
  abstract public SshPacket getPacketfromBytes(
    byte buff[], int offset, int count,boolean encryption,SshCrypto crypto
  ) throws IOException ;

  public boolean toBeFinished = false;
  public  byte[] unfinishedBuffer;
  public int positionInUnfinishedBuffer;
} //class
