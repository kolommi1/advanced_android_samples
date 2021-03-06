package cz.uhk.advanced_android_samples.utils_library.transforms;

/**
 * A 4x4 matrix of right-handed rotation about y-axis
 * 
 * @author PGRF FIM UHK 
 * @version 2016
 */
public class Mat4RotY extends Mat4Identity {

	/**
	 * Creates a 4x4 transformation matrix equivalent to right-handed rotation
	 * about y-axis
	 * 
	 * @param alpha
	 *            rotation angle in radians
	 */
	public Mat4RotY(final double alpha) {
		mat[0][0] = (double) Math.cos(alpha);
		mat[2][2] = (double) Math.cos(alpha);
		mat[2][0] = (double) Math.sin(alpha);
		mat[0][2] = (double) -Math.sin(alpha);
	}
}