package cnuphys.grid;

/**
 * Standard 3D Cartesian vector
 * @author davidheddle
 *
 */
public class FloatVect {
	
	/** the x coordinate */
	public float x;
	
	/** the y coordinate */
	public float y;
	
	/** the z coordinate */
	public float z;

	/**
	 * Create an uninitialized vector whose
	 * components are set to NaN
	 */
	public FloatVect() {
		x = Float.NaN;
		y = Float.NaN;
		z = Float.NaN;
	}

	/**
	 * Create an initialized vector
	 * @param xx the x component
	 * @param yy the y component
	 * @param zz the z component
	 */
	public FloatVect(float xx, float yy, float zz) {
		x = xx;
		y = yy;
		z = zz;
	}

}