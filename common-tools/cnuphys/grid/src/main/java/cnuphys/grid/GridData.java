package cnuphys.grid;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class GridData implements IData {
	
	
	
	/** the base name of the data file */
	private String _baseFileName;

	
	/** the default name of the data */
	protected String name = "GridData";
	
	/**
	 * Holds the grid info for the slowest changing coordinate (as stored in the
	 * file).
	 */
	protected GridCoordinate q1Coordinate;

	/**
	 * Holds the grid info for the medium changing coordinate (as stored in the
	 * file).
	 */
	protected GridCoordinate q2Coordinate;

	/**
	 * Holds the grid info for the fastest changing coordinate (as stored in the
	 * file).
	 */
	protected GridCoordinate q3Coordinate;
	
	/** Index where max data vector magnitude resides */
	protected int maxDataIndex = -1;
	
	/** Data file header */
	private Header _header;
	
	/** holds the field in a float buffer. */
	protected FloatBuffer field;

	/** The maximum magnitude of the data. */
	protected float maxDataMagnitude = Float.NEGATIVE_INFINITY;

	/** The data vector with the maximum magnitude */
	protected final float maxVector[] = new float[3];

	/** The grid location of the maximum vector. */
	protected final float maxVectorLocation[] = new float[3];

	/** The average magnitude of the data vectors */
	protected float avgDataMagnitude = Float.NaN;

	/** The default coordinate 1 name */
	protected String q1Name = "x";

	/** The default coordinate 2 name */
	protected String q2Name = "y";

	/** The default coordinate 3 name */
	protected String q3Name = "z";
	
	/** holds the data in a float buffer. */
	protected FloatBuffer dataBuffer;
	
	/**
	 * Set the names of the coordinates
	 * @param q1Name the name for coordinate 1
	 * @param q2Name the name for coordinate 2
	 * @param q3Name the name for coordinate 3
	 */
	public void setCoordinateNames(String q1Name, String q2Name, String q3Name) {
		this.q1Name = new String(q1Name);
		this.q2Name = new String(q2Name);
		this.q3Name = new String(q3Name);
	}
	
	/**
	 * Get the composite index to take me to the correct place in the buffer.
	 * 
	 * @param n1 the index in the q1 direction
	 * @param n2 the index in the q2 direction
	 * @param n3 the index in the q3 direction
	 * @return the composite index (buffer offset)
	 */
	public final int getCompositeIndex(int n1, int n2, int n3) {
		int n23 = q2Coordinate.getNumPoints() * q3Coordinate.getNumPoints();
		return n1 * n23 + n2 * q3Coordinate.getNumPoints() + n3;
	}

	/**
	 * Convert a composite index back to the coordinate indices
	 * 
	 * @param index    the composite index.
	 * @param qindices the coordinate indices
	 */
	protected final void getCoordinateIndices(int index, int qindices[]) {
		int N3 = q3Coordinate.getNumPoints();
		int n3 = index % N3;
		index = (index - n3) / N3;

		int N2 = q2Coordinate.getNumPoints();
		int n2 = index % N2;
		int n1 = (index - n2) / N2;
		qindices[0] = n1;
		qindices[1] = n2;
		qindices[2] = n3;
	}

	/**
	 * Get the B1 component at a given composite index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the composite index.
	 * @return the B1 at the given composite index.
	 */
	@Override
	public final float getB1(int index) {
		int i = 3 * index;

		try {
			if (i >= dataBuffer.limit()) {
				return 0f;
			}
			float val = dataBuffer.get(i);
			return val;
		} catch (IndexOutOfBoundsException e) {
			System.err.println("error in mag field index1 = " + index);
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Get the B2 component at a given composite index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the composite index.
	 * @return the B2 at the given composite index.
	 */
	@Override
	public final float getB2(int index) {
		int i = 1 + 3 * index;
		if (i >= dataBuffer.limit()) {
			return 0f;
		}
		float val = dataBuffer.get(i);
		return val;
	}

	/**
	 * Get the B3 component at a given composite index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the composite index.
	 * @return the B3 at the given composite index.
	 */
	@Override
	public final float getB3(int index) {
		int i = 2 + 3 * index;
		if (i >= dataBuffer.limit()) {
			return 0f;
		}
		float val = dataBuffer.get(i);
		return val;
	}

	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Set the name of the data
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = new String(name);
	}

	@Override
	public float getMaxMagnitude() {
		return maxDataMagnitude;
	}


	@Override
	public boolean contains(double q1, double q2, double q3) {
		return q1Coordinate.contains(q1) &&
				q2Coordinate.contains(q2) &&
				q3Coordinate.contains(q3);
	}
	
	/**
	 * Get the q1 coordinate.
	 *
	 * @return the q1 coordinate
	 */
	public final GridCoordinate getQ1Coordinate() {
		return q1Coordinate;
	}

	/**
	 * Get the q2 coordinate.
	 *
	 * @return the q2 coordinate
	 */
	public final GridCoordinate getQ2Coordinate() {
		return q2Coordinate;
	}

	/**
	 * Get the q3 coordinate.
	 *
	 * @return the q3 coordinate
	 */
	public final GridCoordinate getQ3Coordinate() {
		return q3Coordinate;
	}
	
	/**
	 * Get a component of the data
	 * @param componentIndex [1..3]
	 * @param compositeIndex
	 * @return the component
	 */
	public double getBComponent(int componentIndex, int compositeIndex) {
		switch (componentIndex) {
		case 1:
			return getB1(compositeIndex);

		case 2:
			return getB2(compositeIndex);
			
		case 3:
			return getB3(compositeIndex);
		
		default: 
			System.err.println("Asked for bad component of the data: [" + componentIndex + "] sould be 1, 2 or 3 only.");
			System.exit(-1);
		}
		
		return Double.NaN;
	}

	/**
	 * Get the square of magnitude of a data vector
	 * for a given index.
	 * 
	 * @param index the index.
	 * @return the square of data vector magnitude at the given index.
	 */
	protected final double squareMagnitude(int index) {
		int i = 3 * index;
		float B1 = dataBuffer.get(i);
		float B2 = dataBuffer.get(i + 1);
		float B3 = dataBuffer.get(i + 2);
		return B1 * B1 + B2 * B2 + B3 * B3;
	}
	
	/**
	 * Get the vector for a given index.
	 * 
	 * @param index the index.
	 * @param vv    an array of three floats to hold the result.
	 */
	protected final void vectorField(int index, float vv[]) {
		int i = 3 * index;
		vv[0] = dataBuffer.get(i);
		vv[1] = dataBuffer.get(i + 1);
		vv[2] = dataBuffer.get(i + 2);
	}
	
	/** compute max data quantities */
	protected void computeMaxDataMagnitude() {
		
		maxDataIndex = -1;
		
		if (_header == null) {
			maxDataMagnitude = Float.NaN;
			avgDataMagnitude = Float.NaN;
			for (int i = 0; i < 3; i++) {
				maxVector[i] = Float.NaN;
				maxVectorLocation[i] = Float.NaN;
			}
			return;
		}

		double maxf = -1.0e10;

		double sum = 0.0;

		for (int i = 0; i < _header.numPoints; i++) {
			double fm = Math.sqrt(squareMagnitude(i));
			sum += fm;

			if (fm > maxf) {
				maxf = fm;
				maxDataIndex = i;
			}
		}
		vectorField(maxDataIndex, maxVector);
		maxDataMagnitude = (float) maxf;
		avgDataMagnitude = (float) sum / _header.numPoints;
		getLocation(maxDataIndex, maxVectorLocation);
	}
	
	/**
	 * Get the location at a given index
	 * 
	 * @param index the composite index
	 * @param r a vector that holds the three data components of the location
	 */
	public final void getLocation(int index, float r[]) {
		int qindices[] = new int[3];
		getCoordinateIndices(index, qindices);
		r[0] = (float) q1Coordinate.getValue(qindices[0]);
		r[1] = (float) q2Coordinate.getValue(qindices[1]);
		r[2] = (float) q3Coordinate.getValue(qindices[2]);
	}
	

	/**
	 * Read the data from a binary file.
	 * Header format (32 bit words)
	 * word        comment
	 * --------------------
	 * 1           q1Min
	 * 2           q1Max
	 * 3           q2Min
	 * 4           q2Max
	 * 5           q3Min
	 * 6           q3Max
	 * 
	 * 
	 * @param binaryFile the binary file.
	 * @throws FileNotFoundException the file not found exception
	 */
	public final void readBinaryData(File binaryFile) throws FileNotFoundException {

		_baseFileName = (binaryFile == null) ? "???" : binaryFile.getName();
		int index = _baseFileName.lastIndexOf(".");
		if (index > 1) {
			_baseFileName = _baseFileName.substring(0, index);
		}

		// N23 = -1;

		try {
			DataInputStream dos = new DataInputStream(new FileInputStream(binaryFile));
			_header = Header.read(dos);
			if (_header == null) {
				System.err.println("Header not read sucessfully from " + binaryFile.getPath());
				return;
			}
			
			q1Coordinate = new GridCoordinate(q1Name, _header.q1Min, _header.q1Max, _header.nQ1);
			q2Coordinate = new GridCoordinate(q2Name, _header.q2Min, _header.q2Max, _header.nQ2);
			q3Coordinate = new GridCoordinate(q3Name, _header.q3Min, _header.q3Max, _header.nQ3);
			
			byte bytes[] = new byte[_header.dataSize];

			// read the bytes as a block
			dos.read(bytes);
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
			field = byteBuffer.asFloatBuffer();

			computeMaxDataMagnitude();

			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Obtain a GridData from a binary file.
	 *
	 * @param file the file to read
	 * @return the GridData object
	 * @throws FileNotFoundException the file not found exception
	 */
	public static GridData fromBinaryFile(File file) throws FileNotFoundException {
		GridData data = new GridData();
		data.readBinaryData(file);

		System.out.println(data.toString());

		return data;
	}


}
