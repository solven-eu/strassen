package io.cormoran.strassen.v2;

import org.junit.Assert;
import org.junit.Test;

public class TestGreek {
	@Test
	public void testSoftlyGrowing() {
		// -1 everywhere
		Assert.assertTrue(Greek.isSoftlyGrowing(new Greek(0)));

		// 0 then -1 everywhere
		Assert.assertFalse(Greek.isSoftlyGrowing(new Greek(1)));
	}
}
