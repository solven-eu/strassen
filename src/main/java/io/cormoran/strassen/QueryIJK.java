package io.cormoran.strassen;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Ints;

public class QueryIJK {
	protected final V5 i;
	protected final V5 j;
	protected final V5 k;

	public QueryIJK(V5 i, V5 j, V5 k) {
		this.i = i;
		this.j = j;
		this.k = k;
	}

	@JsonCreator
	public QueryIJK(@JsonProperty("i") List<Integer> i,
			@JsonProperty("j") List<Integer> j,
			@JsonProperty("k") List<Integer> k) {
		this(new V5(Ints.toArray(i)), new V5(Ints.toArray(j)), new V5(Ints.toArray(k)));
	}

	public List<Integer> getI() {
		return Ints.asList(i.v0);
	}

	public List<Integer> getJ() {
		return Ints.asList(j.v0);
	}

	public List<Integer> getK() {
		return Ints.asList(k.v0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((i == null) ? 0 : i.hashCode());
		result = prime * result + ((j == null) ? 0 : j.hashCode());
		result = prime * result + ((k == null) ? 0 : k.hashCode());
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
		QueryIJK other = (QueryIJK) obj;
		if (i == null) {
			if (other.i != null)
				return false;
		} else if (!i.equals(other.i))
			return false;
		if (j == null) {
			if (other.j != null)
				return false;
		} else if (!j.equals(other.j))
			return false;
		if (k == null) {
			if (other.k != null)
				return false;
		} else if (!k.equals(other.k))
			return false;
		return true;
	}

}
