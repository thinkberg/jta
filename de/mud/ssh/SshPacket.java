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

//setBlock()					//block <- padding + packet_type + data +crc

//getBytes						//return packet_length + encrypted_block

//getPacketfromBytes			//-> packet_length_array and encrypted_block
								// decrypt(encrypted_block) -> decryptedBlock
								// setPacketFromDecryptedBlock
								// checkCrc
								

//setPacketFromDecryptedBlock	//  decryptedBlock -> padding + packet_type + data +crc

//getType

//getData		




import java.io.IOException;



class SshPacket {

  private final static boolean debug = false;

  //SSH_RECEIVE_PACKET
  private byte[] packet_length_array = new byte[4];				// 4 bytes
  private int packet_length = 0;									// 32-bit sign int
  private byte[] padding = null;									// 1-8 bytes of random data (or zeroes if not encrypting).
  private byte packet_type;										// byte : we 'll have to convert a byte to a short(signed/unsigned)
  private short last_packet_type;									
  private byte[] data = null;										// the data ..
  private byte[] crc_array = new byte[4];							// 32-bit crc
  private byte[] block = null;									// (Padding + Type + Data + Check) 
  private byte[] encryptedBlock = null;							// encrypted part (Padding + Type + Data + Check) 
  private byte[] decryptedBlock = null;							// decrypted part (Padding + Type + Data + Check) 


  public boolean toBeFinished = false;
  public  byte[] unfinishedBuffer;
  public int positionInUnfinishedBuffer;


  //create an encrypted packey from newData and newType 
  public SshPacket(byte newType, byte[] newData,boolean encryption,SshCrypto crypto) throws IOException { 

    data = newData;
    packet_type = newType;

    //packet length
    if (data!=null) packet_length = data.length + 5;
    else packet_length = 5;
    packet_length_array[3] = (byte) (packet_length & 0xff);
    packet_length_array[2] = (byte) ((packet_length>>8) & 0xff);
    packet_length_array[1] = (byte) ((packet_length>>16) & 0xff);
    packet_length_array[0] = (byte) ((packet_length>>24) & 0xff);
		
    //padding
    padding = new byte[(8 -(packet_length %8))];
    if (!encryption) for(int i=0; i<padding.length; i++) padding[i] = 0;
    else { for(int i=0; i<padding.length; i++) padding[i] = SshMisc.getNotZeroRandomByte();}

		  
    //Compute the crc of [ Padding, Packet type, Data ]
		
    byte[] tempByte = new byte[packet_length + padding.length - 4];
    int offset =0;
    for(int i=0; i<padding.length; i++) tempByte[offset++] = padding[i];
    tempByte[offset++] = packet_type;
    if(packet_length > 5) for(int i=0; i<data.length; i++) tempByte[offset++] = data[i];

    long crc = 0;
    crc = SshMisc.crc32(tempByte, tempByte.length);
    crc_array[3] = (byte) (crc & 0xff);
    crc_array[2] = (byte) ((crc>>8) & 0xff);
    crc_array[1] = (byte) ((crc>>16) & 0xff);
    crc_array[0] = (byte) ((crc>>24) & 0xff);
		
    //encrypt		
    setBlock();
    if (encryption) crypto.encrypt(block);

    encryptedBlock = block;
		
  };




  //block = padding + packet_type + data +crc
  private void setBlock() throws IOException {

    block = new byte[packet_length + padding.length];
    int blockOffset =0;
    for(int i=0; i<padding.length; i++) block[blockOffset++] = padding[i];
    block[blockOffset++] = packet_type;
    if(packet_length > 5)  for(int i=0; i<data.length; i++) block[blockOffset++] = data[i];
    for(int i=0; i<crc_array.length; i++) block[blockOffset++] = crc_array[i];
  };
	
	


  public byte[] getBytes() throws IOException {

    return SshMisc.addArrayOfBytes(packet_length_array, encryptedBlock);

  };


  public byte[] getData() throws IOException {	return data; };

  public byte getType() throws IOException {	return packet_type; };

	
	
  private int position = 0;
	
  private int phase_packet 		=	0;
  private final int PHASE_packet_length =	0;	
  private final int PHASE_block 	=	1;


  public SshPacket getPacketfromBytes(byte buff[], int offset, int count,boolean encryption,SshCrypto crypto) 
    throws IOException {

    int boffset = offset;
    byte b;  			
    while(boffset < count) {
      b=buff[boffset++];	
      switch (phase_packet) {
			
	// 4 bytes				
	// Packet length: 32 bit unsigned integer 
	// gives the length of the packet, not including the length field 
	// and padding.  maximum is 262144 bytes.
			
      case PHASE_packet_length:
	packet_length_array[position] = b;
	if (++position>=4) {
	  packet_length =
	     (packet_length_array[3]&0xff) +  
	    ((packet_length_array[2]&0xff)<<8) +
	    ((packet_length_array[1]&0xff)<<16) +
	    ((packet_length_array[0]&0xff)<<24); 
	  position=0;
	  phase_packet++; 
	}
	break; //switch (phase_packet)

	
	//8*(packet_length/8 +1) bytes

      case PHASE_block  :

	if (position==0) {	
	  //packet_length - packet_length%8 +8;
	  int lengthBlock = 8*(packet_length/8 +1);
	  block = new byte[lengthBlock];

	}
					
					
	block[position] = b;
				
	if (++position>=block.length) { //the block is complete
	  if (count>boffset) {  //there is more than 1 packet in buff
	    toBeFinished = true;
	    unfinishedBuffer = buff;
	    positionInUnfinishedBuffer = boffset;
	  }
	  else toBeFinished = false;

	  position=0;
	  phase_packet = PHASE_packet_length;
						
	  if (encryption) crypto.decrypt(block);
	  decryptedBlock = block;
	  setPacketFromDecryptedBlock();		
	  if (!checkCrc()) {
	    System.err.println("SshPacket: Crc Error !!");
	    return null;
	  }
			
	  return this;
	} //if (++position>=blo...
					
	break;


      } // switch (phase_packet) 
    } //while 
    toBeFinished = false;	//of course :-)
    return null;			//the packet needs more data...
  };

	

	
	
	
  private boolean checkCrc(){

    byte[] crc_arrayCheck = new byte[4];							// 32-bit crc
    long crcCheck;											// 32-bit crc

    crcCheck = SshMisc.crc32(decryptedBlock, decryptedBlock.length-4);
    crc_arrayCheck[3] = (byte) (crcCheck & 0xff);
    crc_arrayCheck[2] = (byte) ((crcCheck>>8) & 0xff);
    crc_arrayCheck[1] = (byte) ((crcCheck>>16) & 0xff);
    crc_arrayCheck[0] = (byte) ((crcCheck>>24) & 0xff);

    if(debug) {
      System.err.println(crc_arrayCheck[3]+" == "+crc_array[3]);
      System.err.println(crc_arrayCheck[2]+" == "+crc_array[2]);
      System.err.println(crc_arrayCheck[1]+" == "+crc_array[1]);
      System.err.println(crc_arrayCheck[0]+" == "+crc_array[0]);
    }

    if	(crc_arrayCheck[3] != crc_array[3]) return false; 
    if	(crc_arrayCheck[2] != crc_array[2]) return false;
    if	(crc_arrayCheck[1] != crc_array[1]) return false;
    if	(crc_arrayCheck[0] != crc_array[0]) return false;
    return true;


  }
	
  // the block is decrypted
  // packet_length has already been set 

  private void setPacketFromDecryptedBlock() throws IOException { 
		
    int blockOffset = 0; 

    //padding
    int padding_length = (int) (8-(packet_length%8));
    padding = new byte[padding_length];

    if (decryptedBlock.length != padding_length + packet_length)
      System.out.println("???");

    for (int i=0; i<padding.length; i++) padding[i] = decryptedBlock[blockOffset++];
	
    //packet type
    packet_type = decryptedBlock[blockOffset++];
    if(debug) System.err.println("Packet type: "+packet_type);
				
    //data
    if(packet_length > 5)  {
      data = new byte[packet_length-5];
      for(int i=0; i<data.length; i++) data[i] = decryptedBlock[blockOffset++];
    }
    else data = null;
		
    //crc
    for (int i=0; i<crc_array.length; i++) crc_array[i] = decryptedBlock[blockOffset++];
  };

	
	
	
	
} //class
