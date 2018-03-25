package io.cormoran.strassen;

import java.util.Set;

public class IJKLAndAEs {
	public final QueryIJKL ijkl;
	public final Set<V5> aeCandidates;

	public IJKLAndAEs(QueryIJKL ijkl, Set<V5> aeCandidates) {
		this.ijkl = ijkl;
		this.aeCandidates = aeCandidates;
	}

	@Override
	public String toString() {
		return "IJKLAndAEs [ijkl=" + ijkl + ", aeCandidates=" + aeCandidates + "]";
	}

}
