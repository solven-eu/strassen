package io.cormoran.strassen.v2;

import java.util.Objects;

@Deprecated
public class V4V4 {
	public final ABCD a;
	public final ABCD efgh;

	public V4V4(ABCD a, ABCD efgh) {
		this.a = a;
		this.efgh = efgh;
	}

	@Override
	public int hashCode() {
		return Objects.hash(a, efgh);
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
		V4V4 other = (V4V4) obj;
		return Objects.equals(a, other.a) && Objects.equals(efgh, other.efgh);
	}

}
