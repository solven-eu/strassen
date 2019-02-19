package io.cormoran.strassen.v2;

/**
 * Includes various helpers for operations on Vectors
 * 
 * @author Benoit Lacelle
 *
 */
public class VectorOperations {
	public static int scalarProduct(V4 v4, AE4 ae4) {
		int scalar = 0;

		for (int i = 0; i < V4.NB_BLOCK; i++) {
			scalar += v4.getI(i) * ae4.getI(i);

		}
		return scalar;
	}

	public static int scalarProduct(V4 left, V4 right) {
		int scalar = 0;

		for (int i = 0; i < V4.NB_BLOCK; i++) {
			scalar += left.getI(i) * right.getI(i);

		}
		return scalar;
	}

	public static AE4 mul(V4 v4, int coeff) {
		int value = 0;
		for (int i = 0; i < V4.NB_BLOCK; i++) {
			value += (v4.getI(i) * coeff + AE4.MAX_AE) * Math.pow(AE4.NB_VALUES_AE, i);
		}

		return new AE4(value);
	}

	public static AE4 mul(V4 left, V4 right) {
		int value = 0;
		for (int i = 0; i < V4.NB_BLOCK; i++) {
			value += (left.getI(i) * right.getI(i) + AE4.MAX_AE) * Math.pow(AE4.NB_VALUES_AE, i);
		}

		return new AE4(value);
	}
}
