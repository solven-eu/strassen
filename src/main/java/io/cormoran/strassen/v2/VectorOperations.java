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
}
