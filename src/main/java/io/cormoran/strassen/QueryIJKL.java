package io.cormoran.strassen;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Ints;

public class QueryIJKL {
	protected final V5 i;
	protected final V5 j;
	protected final V5 k;
	protected final V5 l;

	public QueryIJKL(V5 i, V5 j, V5 k, V5 l) {
		this.i = i;
		this.j = j;
		this.k = k;
		this.l = l;
	}

	@JsonCreator
	public QueryIJKL(@JsonProperty("i") List<Integer> i,
			@JsonProperty("j") List<Integer> j,
			@JsonProperty("k") List<Integer> k,
			@JsonProperty("l") List<Integer> l) {
		this(new V5(Ints.toArray(i)), new V5(Ints.toArray(j)), new V5(Ints.toArray(k)), new V5(Ints.toArray(l)));
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

	public List<Integer> getL() {
		return Ints.asList(l.v0);
	}
}
