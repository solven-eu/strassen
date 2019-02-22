package io.cormoran.strassen.v2;

import org.junit.Assert;
import org.junit.Test;

public class TestOtherStrassen {
	@Test(expected = Throwable.class)
	public void testM4_NegativeIndex() {
		new M4(-1, 0);
	}

	@Test(expected = Throwable.class)
	public void testM4_TooBigIndex() {
		new M4(81, 0);
	}

	@Test
	public void testV4_getI_0() {
		ABCD v4_0 = new ABCD(0);
		Assert.assertEquals(-A.MAX_VALUE, v4_0.getI(2));
	}

	@Test
	public void testV4_getI_50() {
		ABCD v4_50 = new ABCD(50);
		Assert.assertEquals(1, v4_50.getI(2));
	}

	@Test
	public void testScalarMul() {
		ABCD base = new ABCD(0);
		int coeff = -1;
		AE4 multiplied = VectorOperations.mul(base, coeff);

		for (int i = 0; i < ABCD.NB_BLOCK; i++) {
			Assert.assertEquals(base.getI(i) * coeff, multiplied.getI(i));
		}
	}
}
