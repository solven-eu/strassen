package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.primitives.Ints;

/**
 * This Vector represent (M0|A, M1|A, ..., Mn|A), which are the coefficient taken by ABCD on A
 * 
 * @author Benoit Lacelle
 *
 */
public class A extends Vector {
	private static final long serialVersionUID = 2071895882407701454L;

	// If 1, we allow from -1 to 1. If 2, we allow from -2 to 2
	public static final int MAX_VALUE = 1;

	// If A can range from -2 to 2, then AE can range from -4 to 4
	public static final int MAX_A = (int) Math.pow(MAX_VALUE, 2);

	// From -max to +max, including 0
	public static final int NB_VALUES_A = MAX_A * 2 + 1;

	// How many underlying multiplications we allow to process
	// 4 leads to no solution
	public static final int NB_MUL = AE4.NB_MUL;

	// The maximum acceptable index
	public static final int NB_A_WING = Ints.checkedCast((long) Math.pow(NB_VALUES_A, NB_MUL));

	public A(int index) {
		super(NB_MUL, MAX_VALUE, index);
	}

	public static A fromValues(int... values) {
		int value = Vector.computeIndex(A.NB_MUL, A.MAX_A, values);

		return new A(value);
	}

	public static A zero() {
		return fromValues(new int[NB_MUL]);
	}

}
