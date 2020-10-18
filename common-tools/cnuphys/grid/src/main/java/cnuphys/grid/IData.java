package cnuphys.grid;

public interface IData {

	/**
	 * Get B1 at a given index.
	 * "B" is the generic name for the 3D vector
	 * stored at each grid point.
	 * 
	 * @param index the index into the data buffer.
	 * @return the B1 at the given index.
	 */
	public float getB1(int index);

	/**
	 * Get B2 at a given index.
	 * 
	 * @param index the index into the data buffer.
	 * @return the B2 at the given index.
	 */
	public float getB2(int index);

	/**
	 * Get B3 at a given index.
	 * "B" is the generic name for the 3D vector
	 * 
	 * @param index the index into the data buffer.
	 * @return the B3 at the given index.
	 */
	public float getB3(int index);

	/**
	 * Get the name of the data
	 * 
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Obtain the maximum magnitude of any vector on the grid.
	 * 
	 * @return the maximum magnitude of any vector on the grid.
	 */
	public float getMaxMagnitude();

	/**
	 * Checks whether the grid boundary contains the given point.
	 * 
	 * @param q1      the q1 coordinate 
	 * @param q2      the q2 coordinate 
	 * @param q3      the q3 coordinate 
	 * @return <code>true</code> if the grid contains the given point
	 */
	public boolean contains(double q1, double q2, double q3);

}
