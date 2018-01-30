package io.cormoran.strassen;
import com.google.common.util.concurrent.AtomicLongMap;

public final class LinearSum {
	protected final AtomicLongMap<PairOfBLock> pairWithCoef;

	public LinearSum() {
		pairWithCoef = AtomicLongMap.create();
	}

	protected LinearSum(AtomicLongMap<PairOfBLock> clone) {
		pairWithCoef = clone;
	}

	public LinearSum add(int count, PairOfBLock pair) {
		AtomicLongMap<PairOfBLock> clone = AtomicLongMap.create(pairWithCoef.asMap());
		long newCount = clone.addAndGet(pair, count);
		if (newCount == 0) {
			clone.removeIfZero(pair);
		}

		return new LinearSum(clone);
	}

	public LinearSum merge(LinearSum right) {
		AtomicLongMap<PairOfBLock> clone = AtomicLongMap.create(pairWithCoef.asMap());

		clone.putAll(right.pairWithCoef.asMap());
		clone.removeAllZeros();

		return new LinearSum(clone);
	}

	@Override
	public String toString() {
		return "LinearSum [pairWithCoef=" + pairWithCoef + "]";
	}

	public void scale(long scoreToRemove) {
		if (scoreToRemove == 0) {
			throw new IllegalArgumentException("Can not scale with 0");
		}

		pairWithCoef.asMap().keySet().stream().forEach(
				pair -> pairWithCoef.accumulateAndGet(pair, scoreToRemove, (l, r) -> l * r));
	}

	public void mutateMinus(LinearSum linearSum) {
		linearSum.pairWithCoef.asMap().forEach((pair, score) -> {
			this.pairWithCoef.addAndGet(pair, score);
		});

		// We typically tried to zerofy at leat one key
		linearSum.pairWithCoef.removeAllZeros();
	}

}
