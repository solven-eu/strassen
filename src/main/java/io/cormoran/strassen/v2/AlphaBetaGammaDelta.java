package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A Tuple (Alpha, Beta, Gamma, Delta)
 * 
 * @author Benoit Lacelle
 *
 */
public class AlphaBetaGammaDelta implements Serializable {
	private static final long serialVersionUID = 1896848489651100632L;

	// Number of quadrants: each greek is the linear combination of sub-matrices combinations giving the quadrant
	static final int NB_GREEK = ABCD.NB_BLOCK;

	final Greek[] greeks;

	public AlphaBetaGammaDelta(Greek... greeks) {
		if (greeks.length != NB_GREEK) {
			throw new IllegalStateException("Received: " + greeks.length);
		}
		this.greeks = greeks;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(greeks);
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
		AlphaBetaGammaDelta other = (AlphaBetaGammaDelta) obj;
		return Arrays.equals(greeks, other.greeks);
	}

	public Greek getI(int i) {
		return greeks[i];
	}

	@Override
	public String toString() {
		return "AlphaBetaGammaDelta [greeks=" + Arrays.toString(greeks) + "]";
	}

}
