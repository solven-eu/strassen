package io.cormoran.strassen.v2;

import java.io.Serializable;

import com.google.common.primitives.Ints;

/**
 * This Vector represent Alpha_0, ..., Alpha_N where Alpha.AE gives AE value on Alpha quadrant.
 * 
 * @author Benoit Lacelle
 *
 */
public class Greek extends Vector implements Serializable {
	private static final long serialVersionUID = -7277232825394300174L;

	// If 1, we allow from -1 to 1. If 2, we allow from -2 to 2
	public static final int MAX_VALUE = 1;
	// From -max to +max, including 0
	private static final int NB_VALUES = MAX_VALUE * 2 + 1;

	// For each of the Nth allowed multiplication, we need an Alpha coefficient to weight the quadrant
	public static final int NB_COEF = AE4.NB_MUL;

	public static final int CARDINALITY_GREEK = Ints.checkedCast((long) Math.pow(NB_VALUES, NB_COEF));

	public Greek(int indexL) {
		super(NB_COEF, MAX_VALUE, indexL);
	}

	public static boolean isSoftlyGrowing(Greek greek) {
		for (int i = 1; i < NB_COEF; i++) {
			int previous = greek.getI(i - 1);
			int current = greek.getI(i);

			if (current < previous) {
				return false;
			}
		}

		return true;
	}

}