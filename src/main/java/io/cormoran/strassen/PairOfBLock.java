package io.cormoran.strassen;

public class PairOfBLock implements Comparable<PairOfBLock> {
	public final Block a;
	public final Block b;

	public PairOfBLock(Block a, Block b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PairOfBLock other = (PairOfBLock) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PairOfBLock [a=" + a + ", b=" + b + "]";
	}

	@Override
	public int compareTo(PairOfBLock o) {
		int diffRowA = this.a.row - o.a.row;
		int diffColumnsA = this.a.column - o.a.column;
		int diffRowB = this.b.row - o.b.row;
		int diffColumnB = this.b.column - o.b.column;

		if (diffRowA != 0) {
			return diffRowA;
		}
		if (diffColumnsA != 0) {
			return diffColumnsA;
		}
		if (diffRowB != 0) {
			return diffRowB;
		}
		if (diffColumnB != 0) {
			return diffColumnB;
		}

		return 0;
	}

}
