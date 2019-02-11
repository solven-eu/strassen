package io.cormoran.strassen.v2;

import java.util.Comparator;

/**
 * We are able to compare M4 as we will order them, in order to get ride of some symmetries
 * 
 * @author Benoit Lacelle
 *
 */
public class M4Comparator implements Comparator<M4> {

	@Override
	public int compare(M4 left, M4 right) {
		int compareLeft = Integer.compare(left.left.index, right.left.index);
		if (compareLeft != 0) {
			return compareLeft;
		} else {
			return Integer.compare(left.right.index, right.right.index);
		}
	}

}
