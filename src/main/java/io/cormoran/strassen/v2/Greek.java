package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.primitives.Ints;

/**
 * This Vector represent Alpha_0, ..., Alpha_N where Alpha.AE gives AE value on Alpha quadrant.
 * 
 * @author Benoit Lacelle
 *
 */
public class Greek implements Serializable {
	private static final long serialVersionUID = -7277232825394300174L;

	// If 1, we allow from -1 to 1. If 2, we allow from -2 to 2
	public static final int MAX_VALUE = 1;
	// From -max to +max, including 0
	private static final int NB_VALUES = MAX_VALUE * 2 + 1;

	// For each of the Nth allowed multiplication, we need an Alpha coefficient to weight the quadrant
	public static final int NB_COEF = AE4.NB_MUL;

	public static final int CARDINALITY_GREEK = Ints.checkedCast((long) Math.pow(NB_VALUES, NB_COEF));

	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final int index;

	public Greek(int indexL) {
		assert indexL >= 0;
		assert indexL < CARDINALITY_GREEK;

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
		Greek other = (Greek) obj;
		return index == other.index;
	}

	@Override
	public String toString() {
		return A.toString(NB_COEF, this::getI);
	}

	public int getI(int i) {
		assert i >= 0;
		assert i < NB_COEF;

		long divisor = (long) Math.pow(NB_VALUES, i);
		return (int) (index / divisor) % NB_VALUES - MAX_VALUE;
	}

}