package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.primitives.Ints;

/**
 * This Vector represent (M0|AE, M1|AE, ..., Mn|AE), which are the coefficient taken by Mx matrices along one of the
 * expanded coefficients
 * 
 * @author Benoit Lacelle
 *
 */
public class AE4 implements Serializable {
	private static final long serialVersionUID = 2071895882407701454L;

	public static final int MAX_AE = (int) Math.pow(V4.MAX_VALUE, 2);

	// From -max to +max, including 0
	public static final int NB_VALUES_AE = MAX_AE * 2 + 1;

	public static final int NB_AE_WING = Ints.checkedCast((long) Math.pow(NB_VALUES_AE, V4.NB_BLOCK));

	final int index;

	public AE4(int index) {
		assert index >= 0;
		assert index < NB_AE_WING;

		this.index = index;
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
		AE4 other = (AE4) obj;
		return index == other.index;
	}

	@Override
	public String toString() {
		return "A=" + getAE0() + " B=" + getAE1() + " C=" + getAE2() + " D=" + getAE3();
	}

	public int getI(int i) {
		assert i >= 0;
		assert i < V4.NB_BLOCK;

		return (int) (index / (Math.pow(NB_VALUES_AE, i))) % NB_VALUES_AE - MAX_AE;
	}

	public int getAE0() {
		return index % NB_VALUES_AE - MAX_AE;
	}

	public int getAE1() {
		return (index / NB_VALUES_AE) % NB_VALUES_AE - MAX_AE;
	}

	public int getAE2() {
		return (index / (NB_VALUES_AE * NB_VALUES_AE)) % NB_VALUES_AE - MAX_AE;
	}

	public int getAE3() {
		return (index / (NB_VALUES_AE * NB_VALUES_AE * NB_VALUES_AE)) % NB_VALUES_AE - MAX_AE;
	}
}
