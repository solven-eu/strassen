package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Tuple (Alpha, Beta, Gamma, Delta)
 * 
 * @author Benoit Lacelle
 *
 */
public class AE_AF_CE_CF implements Serializable {
	private static final long serialVersionUID = 7803833135742047673L;

	final AE4 ae;
	final AE4 af;
	final AE4 ce;
	final AE4 cf;

	public AE_AF_CE_CF(AE4 ae, AE4 af, AE4 ce, AE4 cf) {
		this.ae = ae;
		this.af = af;
		this.ce = ce;
		this.cf = cf;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ae, af, ce, cf);
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
		AE_AF_CE_CF other = (AE_AF_CE_CF) obj;
		return Objects.equals(ae, other.ae) && Objects.equals(af, other.af)
				&& Objects.equals(ce, other.ce)
				&& Objects.equals(cf, other.cf);
	}

}
