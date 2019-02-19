package io.cormoran.strassen.v2;

import org.junit.Assert;
import org.junit.Test;

public class TestOtherStrassen {
	@Test
	public void testM4_getA() {
		Assert.assertEquals(-1, new M4(0, 0).getA());
		Assert.assertEquals(0, new M4(1, 0).getA());
		Assert.assertEquals(1, new M4(2, 0).getA());

		Assert.assertEquals(-1, new M4(3, 0).getA());
	}

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
		V4 v4_0 = new V4(0);
		Assert.assertEquals(-V4.MAX_VALUE, v4_0.getI(2));
	}

	@Test
	public void testV4_getI_50() {
		V4 v4_50 = new V4(50);
		Assert.assertEquals(1, v4_50.getI(2));
	}

	@Test
	public void testScalarMul() {
		V4 base = new V4(0);
		int coeff = -1;
		AE4 multiplied = VectorOperations.mul(base, coeff);

		for (int i = 0; i < V4.NB_BLOCK; i++) {
			Assert.assertEquals(base.getI(i) * coeff, multiplied.getI(i));
		}
	}
}
