package io.cormoran.strassen;

import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class TestStrassen {
	@Test
	public void testIsBefore() {
		V5 zero = new V5(IntStream.range(0, 5).map(i -> 0).toArray());
		V5 one = new V5(IntStream.range(0, 5).map(i -> 1).toArray());

		Assert.assertTrue(one.isStrictlyAfter(zero));
		Assert.assertFalse(zero.isStrictlyAfter(one));

		Assert.assertFalse(zero.isStrictlyAfter(zero));
	}
}
