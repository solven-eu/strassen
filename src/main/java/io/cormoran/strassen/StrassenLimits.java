package io.cormoran.strassen;

public class StrassenLimits {

	public final int power;

	public final int minValue;
	public final int maxValue;

	// -1, 0, 1 -> 3 values
	public final int nbValues;

	public StrassenLimits(int nbMultiplications, int maxCoef) {
		power = nbMultiplications;

		maxValue = Math.abs(maxCoef);
		minValue = -1 * maxValue;

		nbValues = maxValue - minValue + 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxValue;
		result = prime * result + minValue;
		result = prime * result + nbValues;
		result = prime * result + power;
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
		StrassenLimits other = (StrassenLimits) obj;
		if (maxValue != other.maxValue)
			return false;
		if (minValue != other.minValue)
			return false;
		if (nbValues != other.nbValues)
			return false;
		if (power != other.power)
			return false;
		return true;
	}

}
