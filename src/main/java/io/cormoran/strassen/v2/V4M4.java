package io.cormoran.strassen.v2;

import java.util.Objects;

public class V4M4 {
	public final V4 a;
	public final M4 efgh;

	public V4M4(V4 a, M4 efgh) {
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
		V4M4 other = (V4M4) obj;
		return Objects.equals(a, other.a) && Objects.equals(efgh, other.efgh);
	}

}
