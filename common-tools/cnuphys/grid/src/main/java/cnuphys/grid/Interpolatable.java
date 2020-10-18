package cnuphys.grid;

public interface Interpolatable {

	/**
	 * Obtain the interpolated grid data at a given location.
	 *
	 * @param q1      the q1 coordinate 
	 * @param q2      the q2 coordinate 
	 * @param q3      the q3 coordinate 
	 * @param result a float array holding the interpolated values. The 0,1
	 *               and 2 indices correspond to q1, q2, and q3 components.
	 */
	public void field(float q1, float q2, float q3, float result[]);
	
	/**
	 * Check whether the grid boundaries include the point
	 * 
	 * @param q1      the q1 coordinate 
	 * @param q2      the q2 coordinate 
	 * @param q3      the q3 coordinate 
	 * @return <code>true</code> if the point is included in the boundary of the
	 *         grid
	 */
	public boolean contains(double q1, double q2, double q3);


}
