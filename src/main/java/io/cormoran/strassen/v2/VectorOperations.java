package io.cormoran.strassen.v2;

import java.util.stream.IntStream;

/**
 * Includes various helpers for operations on Vectors
 * 
 * @author Benoit Lacelle
 *
 */
public class VectorOperations {
	public static int scalarProduct(Greek v4, AE4 ae4) {
		int scalar = 0;

		for (int i = 0; i < Greek.NB_COEF; i++) {
			scalar += v4.getI(i) * ae4.getI(i);

		}
		return scalar;
	}

	public static int scalarProduct(ABCD left, ABCD right) {
		int scalar = 0;

		for (int i = 0; i < ABCD.NB_BLOCK; i++) {
			scalar += left.getI(i) * right.getI(i);

		}
		return scalar;
	}

	public static AE4 mul(ABCD v4, int coeff) {
		return new AE4(AE4.computeIndex(IntStream.range(0, ABCD.NB_BLOCK).map(i -> v4.getI(i) * coeff).toArray()));
	}

	public static AE4 mul(A left, A right) {
		int[] array = IntStream.range(0, A.NB_MUL).map(i -> left.getI(i) * right.getI(i)).toArray();
		return new AE4(AE4.computeIndex(array));
	}
}
