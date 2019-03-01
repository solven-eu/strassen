package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.primitives.Ints;

/**
 * This Vector represent (M0|AE, M1|AE, ..., Mn|AE), which are the coefficient taken by Mx matrices along one of the
 * expanded coefficients
 * 
 * @author Benoit Lacelle
 *
 */
public class AE4 extends Vector {
	private static final long serialVersionUID = 2071895882407701454L;

	// If A can range from -2 to 2, then AE can range from -4 to 4
	public static final int MAX_AE = (int) Math.pow(A.MAX_VALUE, 2);

	// From -max to +max, including 0
	public static final int NB_VALUES_AE = MAX_AE * 2 + 1;

	// How many underlying multiplications we allow to process
	// 4 leads to no solution
	public static final int NB_MUL = 5;

	// The maximum acceptable index
	public static final int NB_AE_WING = Ints.checkedCast((long) Math.pow(NB_VALUES_AE, NB_MUL));

	public AE4(int index) {
		super(NB_MUL, A.MAX_A, index);
	}

	public static AE4 fromValues(int... values) {
		int value = computeIndex(values);

		return new AE4(value);
	}

	public static int computeIndex(int... values) {
		if (values.length != NB_MUL) {
			throw new IllegalArgumentException("We expected to receive ");
		}

		int value = 0;
		for (int i = 0; i < NB_MUL; i++) {
			if (Math.abs(values[i]) > AE4.MAX_AE) {
				throw new IllegalArgumentException(values[i] + " is not in the ange defined by " + AE4.MAX_AE);
			}
			value += (values[i] + AE4.MAX_AE) * Math.pow(AE4.NB_VALUES_AE, i);
		}
		return value;
	}

	public static AE4 zero() {
		return fromValues(new int[NB_MUL]);
	}

}
