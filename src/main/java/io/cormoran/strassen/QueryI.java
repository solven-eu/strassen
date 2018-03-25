package io.cormoran.strassen;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Ints;

public class QueryI {
	protected final V5 i;

	public QueryI(V5 i) {
		this.i = i;
	}

	@JsonCreator
	public QueryI(@JsonProperty("i") List<Integer> i) {
		this(new V5(Ints.toArray(i)));
	}

	public List<Integer> getI() {
		return Ints.asList(i.v0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((i == null) ? 0 : i.hashCode());
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
		QueryI other = (QueryI) obj;
		if (i == null) {
			if (other.i != null)
				return false;
		} else if (!i.equals(other.i))
			return false;
		return true;
	}

}
