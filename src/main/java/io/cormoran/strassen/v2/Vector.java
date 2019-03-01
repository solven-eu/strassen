package io.cormoran.strassen.v2;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.primitives.Ints;

public class Vector implements Serializable {

	final int coordinatesCardinality;
	final int maxValueOnCoordinate;

	final int vectorCardinality;

	final int index;
	final int[] indexes;

	public Vector(int coordinatesCardinality, int maxValueOnCoordinate, int index) {
		this.coordinatesCardinality = coordinatesCardinality;
		this.maxValueOnCoordinate = maxValueOnCoordinate;
		int coordinateCardinality = 2 * maxValueOnCoordinate + 1;
		this.vectorCardinality = Ints.checkedCast((long) Math.pow(coordinateCardinality, coordinatesCardinality));

		assert index >= 0;
		assert index < vectorCardinality;

		this.index = index;
		this.indexes = IntStream.range(0, coordinatesCardinality)
				.map(coordinateIndex -> slowGetIndex(coordinateCardinality, coordinateIndex, index))
				.toArray();
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
		Vector other = (Vector) obj;
		return index == other.index;
	}

	@Override
	public String toString() {
		return toString(coordinatesCardinality, this::getI);
	}

	public static String toString(int length, IntFunction<Integer> toIndex) {
		return IntStream.range(0, length).mapToObj(i -> {
			int value = toIndex.apply(i);

			if (value == 0) {
				return "~0";
			} else if (value > 0) {
				return "+" + value;
			} else {
				// Prefixed by '-'
				return Integer.toString(value);
			}
		}).collect(Collectors.joining(" "));
	}

	public int getI(int i) {
		assert i >= 0;
		assert i < coordinatesCardinality;

		return indexes[i];
	}

	public int slowGetIndex(int nb, int i, int index) {
		long divided = (long) (index / Math.pow(nb, i));
		return (int) divided % nb - maxValueOnCoordinate;
	}

	public static int computeIndex(int coordinatesCardinality, int maxValueOnCoordinate, int... values) {
		if (values.length != coordinatesCardinality) {
			throw new IllegalArgumentException("We expected to receive ...");
		}

		int coordinateCardinality = 2 * maxValueOnCoordinate + 1;

		int value = 0;
		for (int i = 0; i < coordinatesCardinality; i++) {
			int valueAtCurrentIndex = values[i];

			if (Math.abs(valueAtCurrentIndex) > maxValueOnCoordinate) {
				throw new IllegalStateException();
			}

			value += (valueAtCurrentIndex + maxValueOnCoordinate) * Math.pow(coordinateCardinality, i);
		}
		return value;
	}
}
