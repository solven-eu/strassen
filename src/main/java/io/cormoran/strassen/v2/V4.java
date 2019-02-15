package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.primitives.Ints;

/**
 * This Vector represent (a, b, c and d) in (aA + bB + cC + dD)
 * 
 * @author Benoit Lacelle
 *
 */
public class V4 implements Serializable {
	private static final long serialVersionUID = -7277232825394300174L;

	// If 1, we allow from -1 to 1. If 2, we allow from -2 to 2
	public static final int MAX_VALUE = 1;
	// From -max to +max, including 0
	private static final int NB_VALUES = MAX_VALUE * 2 + 1;

	public static final int NB_BLOCK = 4;

	public static final int NB_V4_WING = Ints.checkedCast((long) Math.pow(NB_VALUES, NB_BLOCK));

	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final int index;

	public V4(int indexL) {
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
		V4 other = (V4) obj;
		return index == other.index;
	}

	@Override
	public String toString() {
		return "A=" + getA() + " B=" + getB() + " C=" + getC() + " D=" + getD();
	}

	public int getI(int i) {
		assert i >= 0;
		assert i < NB_BLOCK;

		long divisor = (long) Math.pow(NB_VALUES, i);
		return (int) (index / divisor) % NB_VALUES - MAX_VALUE;
	}

	public int getA() {
		return index % NB_VALUES - MAX_VALUE;
	}

	public int getB() {
		return (index / NB_VALUES) % NB_VALUES - MAX_VALUE;
	}

	public int getC() {
		return (index / (NB_VALUES * NB_VALUES)) % NB_VALUES - MAX_VALUE;
	}

	public int getD() {
		return (index / (NB_VALUES * NB_VALUES * NB_VALUES)) % NB_VALUES - MAX_VALUE;
	}
}
