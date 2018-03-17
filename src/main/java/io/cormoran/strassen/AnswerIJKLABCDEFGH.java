package io.cormoran.strassen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AnswerIJKLABCDEFGH {
	protected final QueryIJKL ijkl;
	protected final QueryIJKL abcd;
	protected final QueryIJKL efgh;

	@JsonCreator
	public AnswerIJKLABCDEFGH(@JsonProperty("ijkl") QueryIJKL ijkl,
			@JsonProperty("abcd") QueryIJKL abcd,
			@JsonProperty("efgh") QueryIJKL efgh) {
		this.ijkl = ijkl;
		this.abcd = abcd;
		this.efgh = efgh;
	}

	public QueryIJKL getIjkl() {
		return ijkl;
	}

	public QueryIJKL getAbcd() {
		return abcd;
	}

	public QueryIJKL getEfgh() {
		return efgh;
	}
}
