package io.cormoran.strassen.v2;

/**
 * This Matrix is the output of (aA + bB + cC + dD) * (eE + fF + gG + hH)
 * 
 * @author Benoit Lacelle
 *
 */
public class M4 {
	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final ABCD left;

	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final ABCD right;

	public M4(int indexL, int indexR) {
		this.left = new ABCD(indexL);
		this.right = new ABCD(indexR);
	}

	public M4(ABCD left, ABCD right) {
		super();
		this.left = left;
		this.right = right;
	}

}
