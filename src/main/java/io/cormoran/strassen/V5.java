package io.cormoran.strassen;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V5 implements Comparable<V5> {
	protected static final Logger LOGGER = LoggerFactory.getLogger(V5.class);

	public final int[] v0;
	private final int hashcode;

	public V5(int[] v0) {
		this.v0 = v0;
		this.hashcode = Arrays.hashCode(v0);
	}

	@Override
	public String toString() {
		return "V5 [v0=" + Arrays.toString(v0) + "]";
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		V5 other = (V5) obj;
		if (!Arrays.equals(v0, other.v0))
			return false;
		return true;
	}

	public V5 multiply(V5 right) {
		int[] mul = new int[v0.length];

		for (int i = 0; i < mul.length; i++) {
			mul[i] = this.v0[i] * right.v0[i];
		}

		return new V5(mul);
	}

	public int multiplyToScalar(V5 right) {
		int mul = 0;

		for (int i = 0; i < v0.length; i++) {
			mul += this.v0[i] * right.v0[i];
		}

		return mul;
	}

	public V5 increment(int minValue, int maxValue) {
		int[] copy = v0.clone();

		boolean incremented = false;

		for (int i = v0.length - 1; i >= 0; i--) {
			if (copy[i] < maxValue) {
				copy[i]++;
				incremented = true;
				break;
			} else {
				copy[i] = minValue;
			}
		}

		if (!incremented) {
			// TODO: We should never get here, we do not throw to improve stability
			for (int i = 0; i < copy.length; i++) {
				copy[i] = minValue;
			}
		}

		V5 incrementedVector = new V5(copy);
		if (!incremented) {
			LOGGER.warn("Incrementing from {} to {}", this, incrementedVector);
		}
		return incrementedVector;
	}

	public boolean isStrictlyAfter(V5 strictlyBefore) {
		for (int i = 0; i < v0.length; i++) {
			int v0Diff = strictlyBefore.v0[i] - this.v0[i];
			if (v0Diff != 0) {
				return v0Diff < 0;
			}
		}
		// Equals
		return false;
	}

	/**
	 * Sometimes, we want to consider a combination of (v1,v2,v3,v4,v5) only once. One way to to so, is to mark the
	 * vectors with growing values as the only combination amongst all possible (e.g. 123 is growing, and it rejects
	 * 132, 213,231,312,321)
	 * 
	 * @return
	 * @deprecated it should not useful for anything
	 */
	public boolean isSoftlyGrowing() {
		for (int i = 0; i < v0.length - 1; i++) {
			if (v0[i] > v0[i + 1]) {
				return false;
			}
		}

		return true;
	}

	public V5 multiplyByScalar(int factor) {
		return new V5(IntStream.of(this.v0).map(i -> i * factor).toArray());
	}

	@Override
	public int compareTo(V5 other) {
		if (this.isStrictlyAfter(other)) {
			return 1;
		} else if (this.equals(other)) {
			return 0;
		} else {
			return -1;
		}
	}
}
