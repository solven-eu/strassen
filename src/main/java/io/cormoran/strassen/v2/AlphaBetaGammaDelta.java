package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Tuple (Alpha, Beta, Gamma, Delta)
 * 
 * @author Benoit Lacelle
 *
 */
public class AlphaBetaGammaDelta implements Serializable {
	private static final long serialVersionUID = 1896848489651100632L;

	final V4 alpha;
	final V4 beta;
	final V4 gamma;
	final V4 delta;

	public AlphaBetaGammaDelta(V4 alpha, V4 beta, V4 gamma, V4 delta) {
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.delta = delta;
	}

	@Override
	public int hashCode() {
		return Objects.hash(alpha, beta, delta, gamma);
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
		return Objects.equals(alpha, other.alpha) && Objects.equals(beta, other.beta)
				&& Objects.equals(delta, other.delta)
				&& Objects.equals(gamma, other.gamma);
	}

}
