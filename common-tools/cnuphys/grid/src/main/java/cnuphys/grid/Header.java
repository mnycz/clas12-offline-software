package cnuphys.grid;

import java.io.DataInputStream;

/**
 * This is the container for the header of the binary format of a DataGrid object
 * @author davidheddle
 *
 */
public class Header {
	
	/** Magic number used to check if byteswapping is necessary. */
	public static final int MAGICNUMBER = 0xced;
	
	/** minimum value of the q1 coordinate */ 
	public float q1Min;
	
	/** maximum value of the q1 coordinate */ 
	public float q1Max;
	
	/** minimum value of the q2 coordinate */ 
	public float q2Min;
	
	/** maximum value of the q2 coordinate */ 
	public float q2Max;
	
	/** minimum value of the q3 coordinate */ 
	public float q3Min;
	
	/** maximum value of the q3 coordinate */ 
	public float q3Max;
	


	public static Header read(DataInputStream dos) {
		Header header = new Header();
		
		boolean swap = false;
		int magicnum = dos.readInt(); // magic number
		
		System.out.println(String.format("Magic number: %04x", magicnum));

		// TODO handle swapping if necessary
		swap = (magicnum != MAGICNUMBER);
		if (swap) {
			System.err.println("byte swapping required but not yet implemented.");
			dos.close();
			return null;
		}

		//first two int words in header are q1Min and q1 Max
		header.q1Min = dos.readFloat();
		header.q1Max = dos.readFloat();
		int nQ1 = dos.readInt();
		q1Coordinate = new GridCoordinate(q1Name, header.q1Min, header.q1Max, nQ1);

		//next two int words in header are q2Min and q2 Max
		header.q2Min = dos.readFloat();
		header.q2Max = dos.readFloat();
		int nQ2 = dos.readInt();
		q2Coordinate = new GridCoordinate(q2Name, header.q2Min, header.q2Max, nQ2);

		//next two int words in header are q3Min and q3 Max
		header.q3Min = dos.readFloat();
		header.q3Max = dos.readFloat();
		int nQ3 = dos.readInt();
		q3Coordinate = new GridCoordinate(q3Name, header.q3Min, header.q3Max, nQ3);

		//can now compute the number of points
		numPoints = nQ1 * nQ2 * nQ3;

		// next tow words reconstruct creation time
		highTime = dos.readInt();
		lowTime = dos.readInt();
		
		
		reserved1 = dos.readInt();
		reserved2 = dos.readInt();
		reserved3 = dos.readInt();		
		return header;
	}

}
