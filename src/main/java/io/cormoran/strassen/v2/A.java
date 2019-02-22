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
public class A implements Serializable {
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

	final int index;

	public A(int index) {
		assert index >= 0;
		assert index < NB_A_WING;

		this.index = index;
	}

	public static A fromValues(int... values) {
		int value = computeIndex(values);

		return new A(value);
	}

	public static int computeIndex(int... values) {
		if (values.length != NB_MUL) {
			throw new IllegalArgumentException("We expected to receive ");
		}

		int value = 0;
		for (int i = 0; i < NB_MUL; i++) {
			value += (values[i] + A.MAX_A) * Math.pow(A.NB_VALUES_A, i);
		}
		return value;
	}

	public static A zero() {
		return fromValues(new int[NB_MUL]);
	}

	@Override
	public int hashCode() {
		return Objects.hash(index);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		A other = (A) obj;
		return index == other.index;
	}

	@Override
	public String toString() {
		return toString(NB_MUL, this::getI);
	}

	public static String toString(int length, IntFunction<Integer> toIndex) {
		return IntStream.range(0, length).mapToObj(i -> {
			int value = toIndex.apply(i);

			if (value == 0) {
				return "~0";
			} else if (value > 0) {
				return "+" + value;
			} else {
				// Prefixed by '-'
				return Integer.toString(value);
			}
		}).collect(Collectors.joining(" "));
	}

	public int getI(int i) {
		assert i >= 0;
		assert i < NB_MUL;

		double divided = index / Math.pow(NB_VALUES_A, i);
		return (int) divided % NB_VALUES_A - MAX_A;
	}

	@Deprecated
	public int getAE0() {
		return index % NB_VALUES_A - MAX_A;
	}

	@Deprecated
	public int getAE1() {
		return (index / NB_VALUES_A) % NB_VALUES_A - MAX_A;
	}

	@Deprecated
	public int getAE2() {
		return (index / (NB_VALUES_A * NB_VALUES_A)) % NB_VALUES_A - MAX_A;
	}

	@Deprecated
	public int getAE3() {
		return (index / (NB_VALUES_A * NB_VALUES_A * NB_VALUES_A)) % NB_VALUES_A - MAX_A;
	}
}
