package cnuphys.grid;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * This is the container for the header of the binary format of a DataGrid
 * object
 * 
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

	/** number of points (including ends) for q1 cootdinate */
	public int nQ1;

	/** number of points (including ends) for q2 cootdinate */
	public int nQ2;

	/** number of points (including ends) for q3 cootdinate */
	public int nQ3;
	
	/** number of extra columns beyond the default 6 (q1,q1,q2,b1,b2,b3) */
	public int numExtraColumns;

	/** total number of data points */
	public int numPoints;

	/** high word of unix creation time */
	public int highTime;

	/** low word of unix creation time */
	public int lowTime;

	/** reserved word */
	public int reserved1;

	/** reserved word */
	public int reserved2;

	/** reserved word */
	public int reserved3;
	
	/** how big the float data buffer will have to be in bytes */
	public int dataSize;

	public static Header read(DataInputStream dos) {
		Header header = new Header();

		boolean swap = false;

		try {
			int magicnum = dos.readInt(); // magic number

			System.out.println(String.format("Magic number: %04x", magicnum));

			// TODO handle swapping if necessary
			swap = (magicnum != MAGICNUMBER);
			if (swap) {
				System.err.println("byte swapping required but not yet implemented.");
				dos.close();
				return null;
			}

			// first two int words in header are q1Min and q1 Max
			header.q1Min = dos.readFloat();
			header.q1Max = dos.readFloat();
			header.nQ1 = dos.readInt();

			// next two int words in header are q2Min and q2 Max
			header.q2Min = dos.readFloat();
			header.q2Max = dos.readFloat();
			header.nQ2 = dos.readInt();

			// next two int words in header are q3Min and q3 Max
			header.q3Min = dos.readFloat();
			header.q3Max = dos.readFloat();
			header.nQ3 = dos.readInt();
			
			//number of extra columns
			header.numExtraColumns = dos.readInt();

			// can now compute the number of points
			header.numPoints = header.nQ1 * header.nQ2 * header.nQ3;
			
			header.dataSize = 4*(3 + header.numExtraColumns)*header.numPoints;

			// next tow words reconstruct creation time
			header.highTime = dos.readInt();
			header.lowTime = dos.readInt();

			header.reserved1 = dos.readInt();
			header.reserved2 = dos.readInt();
			header.reserved3 = dos.readInt();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return header;
	}

}
