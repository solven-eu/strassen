package io.cormoran.strassen;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Ignore;
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

	@Test
	public void testCheckStrassen() {
		Strassen.power = 7;

		List<List<V5>> count = new LambdaStrassen().countForIJKL(new QueryIJKL(Arrays.asList(1, 0, 0, 1, -1, 0, 1),
				Arrays.asList(0, 0, 1, 0, 1, 0, 0),
				Arrays.asList(0, 1, 0, 1, 0, 0, 0),
				Arrays.asList(1, -1, 1, 0, 0, 1, 0)));

		// Ensure we get ride of symmetries
		// long countAsSet = count.stream().map(list -> {
		// List<V5> copy = list.stream().sorted((l, r) -> r.isStrictlyAfter(l)).collect(Collectors.toList());
		//
		// return list;
		// }).distinct().count();
		// Assert.assertEquals(countAsSet, count.size());

		count.forEach(answer -> {
			Map<String, V5> keyToVector = new LinkedHashMap<>();

			keyToVector.put("i", answer.get(0));
			keyToVector.put("j", answer.get(1));
			keyToVector.put("k", answer.get(2));
			keyToVector.put("l", answer.get(3));

			keyToVector.put("a", answer.get(4));
			keyToVector.put("b", answer.get(5));
			keyToVector.put("c", answer.get(6));
			keyToVector.put("d", answer.get(7));

			keyToVector.put("e", answer.get(8));
			keyToVector.put("f", answer.get(9));
			keyToVector.put("g", answer.get(10));
			keyToVector.put("h", answer.get(11));

			Strassen.FORCE_GROWING.forEach(require -> {
				V5 smaller = keyToVector.get(require.get(0));
				V5 bigger = keyToVector.get(require.get(1));

				if (!bigger.isStrictlyAfter(smaller)) {
					Assert.assertTrue(bigger.isStrictlyAfter(smaller));
				}
			});
		});

		Assert.assertEquals(1, count.size());
	}

	@Ignore("Too slow")
	@Test
	public void testCheckStrassen_AWS() {
		Strassen.power = 7;

		List<List<V5>> count = new LambdaStrassenIJK().countForIJK(new QueryIJK(Arrays.asList(1, 0, 0, 1, -1, 0, 1),
				Arrays.asList(0, 0, 1, 0, 1, 0, 0),
				Arrays.asList(0, 1, 0, 1, 0, 0, 0)
		// , Arrays.asList(1, -1, 1, 0, 0, 1, 0)
		)).collect(Collectors.toList());

		Assert.assertEquals(1, count.size());
	}

}
