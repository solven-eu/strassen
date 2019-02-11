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
	public void testV4_getI() {
		Assert.assertEquals(4, new V4(50).getI(2));
	}
}
