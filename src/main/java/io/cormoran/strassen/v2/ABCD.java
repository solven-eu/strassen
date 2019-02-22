package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.primitives.Ints;

/**
 * This Vector represent (a, b, c and d) in (aA + bB + cC + dD)
 * 
 * @author Benoit Lacelle
 *
 */
public class ABCD implements Serializable {
	private static final long serialVersionUID = -7277232825394300174L;

	// From -max to +max, including 0
	private static final int NB_VALUES = A.MAX_VALUE * 2 + 1;

	// We split height in 2, and width in 2: the initial matrix is split into 4 matrices
	public static final int NB_BLOCK = 2 * 2;

	public static final int NB_V4_WING = Ints.checkedCast((long) Math.pow(NB_VALUES, NB_BLOCK));

	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final int index;

	public ABCD(int indexL) {
		assert indexL >= 0;
		assert indexL < NB_V4_WING;

		this.index = indexL;
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
		ABCD other = (ABCD) obj;
		return index == other.index;
	}

	@Override
	public String toString() {
		return IntStream.range(0, NB_BLOCK).mapToObj(i -> i + "=" + getI(i)).collect(Collectors.joining(" "));
	}

	public int getI(int i) {
		assert i >= 0;
		assert i < NB_BLOCK;

		long divisor = (long) Math.pow(NB_VALUES, i);
		return (int) (index / divisor) % NB_VALUES - A.MAX_VALUE;
	}

}
