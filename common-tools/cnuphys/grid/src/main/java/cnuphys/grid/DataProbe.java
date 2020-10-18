package cnuphys.grid;


public class DataProbe implements Interpolatable {
	
	// cell used to cache corner information
	private Cell3D _cell;
	
	//the data buffer
	protected final IData _data;
	
	// cache the name of the field
	protected String _name;

	/**
	 * Holds the grid info for the slowest changing coordinate. 
	 */
	protected GridCoordinate q1Coordinate;

	/**
	 * Holds the grid info for the medium changing coordinate 
	 */
	protected GridCoordinate q2Coordinate;

	/**
	 * Holds the grid info for the fastest changing coordinate 
	 */
	protected GridCoordinate q3Coordinate;

	/**
	 * Create a probe, which is a thread safe way to use the field
	 * 
	 * @param field the underlying field
	 */
	public DataProbe(IData data) {
		_data = data;
		_cell = new Cell3D(this);
		
		if (_data != null) {
			_name = new String(_data.getName());
		} else { // zero data
			_name = "No Data";
		}

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
	 * Get the underlying data buffer
	 * 
	 * @return the data that backs this probe
	 */
	public IData getDataBuffer() {
		return _data;
	}

	/**
	 * Obtain the interpolated data value (vector) at a given location.
	 *
	 * @param q1      the q1 coordinate 
	 * @param q2      the q2 coordinate 
	 * @param q3      the q3 coordinate 
	 * @param result a float array holding the interpolated values. The 0,1
	 *               and 2 indices correspond to q1, q2, and q3 components.
	 */
	@Override
	public void field(float q1, float q2, float q3, float result[]) {
		_cell.calculate(q1, q2, q3, result);
	}

	@Override
	public boolean contains(double q1, double q2, double q3) {
		return _data.contains(q1, q2, q3);
	}
	
	/**
	 * Get B1 at a given index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the index.
	 * @return the B1 at the given index.
	 */
	public final float getB1(int index) {
		return _data.getB1(index);
	}

	/**
	 * Get B2 at a given index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the index.
	 * @return the B2 at the given index.
	 */
	public final float getB2(int index) {
		return _data.getB2(index);
	}

	/**
	 * Get B3 at a given index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the index.
	 * @return the B3 at the given index.
	 */
	public final float getB3(int index) {
		return _data.getB3(index);
	}


}
