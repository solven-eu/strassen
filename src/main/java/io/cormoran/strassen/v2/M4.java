package io.cormoran.strassen.v2;

/**
 * This Matrix is the output of (aA + bB + cC + dD) * (eE + fF + gG + hH)
 * 
 * @author Benoit Lacelle
 *
 */
public class M4 {
	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final V4 left;

	// From 0 to 80 (where 80 = 3*3*3*3 - 1)
	final V4 right;

	public M4(int indexL, int indexR) {
		this.left = new V4(indexL);
		this.right = new V4(indexR);
	}

	public M4(V4 left, V4 right) {
		super();
		this.left = left;
		this.right = right;
	}

	public int getA() {
		return left.getA();
	}

	public int getB() {
		return left.getB();
	}

	public int getC() {
		return left.getC();
	}

	public int getD() {
		return left.getD();
	}

	public int getE() {
		return right.getA();
	}

	public int getF() {
		return right.getB();
	}

	public int getG() {
		return right.getC();
	}

	public int getH() {
		return right.getD();
	}
}
