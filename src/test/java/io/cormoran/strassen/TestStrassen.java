package io.cormoran.strassen;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

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

		QueryIJKL strassenIJKL = new QueryIJKL(Arrays.asList(1, 0, 0, 1, -1, 0, 1),
				Arrays.asList(0, 1, 0, 1, 0, 0, 0),
				Arrays.asList(0, 0, 1, 0, 1, 0, 0),
				Arrays.asList(1, -1, 1, 0, 0, 1, 0));

		List<V5> unsortedStrassenIJKL = Arrays.asList(strassenIJKL.i, strassenIJKL.j, strassenIJKL.k, strassenIJKL.l);
		List<V5> sortedStrassenIJKL = unsortedStrassenIJKL.stream().sorted().collect(Collectors.toList());

		V5 strassenOrderedI = sortedStrassenIJKL.get(0);
		int indexOfIinUnoredred = unsortedStrassenIJKL.indexOf(strassenOrderedI);

		// We observe the answer is 1: it means I in ordered is K in standard strassen
		Assert.assertEquals(2, indexOfIinUnoredred);

		{
			V5 strassenOrderedL = sortedStrassenIJKL.get(3);
			V5 zero = new V5(new int[strassenOrderedL.v0.length]);
			List<V5> orderedNotZero = Strassen.orderedNotZero(LambdaStrassen.leftToRightGiving0);

			Assert.assertTrue("I is not positive", strassenOrderedI.isStrictlyAfter(zero));

			Assert.assertTrue(Strassen.checkSymetryFromIToL(zero,
					orderedNotZero,
					strassenOrderedI,
					orderedNotZero.indexOf(strassenOrderedL)));
		}

		long nbSolutions = Strassen.allIJKLAsStream(LambdaStrassen.leftToRightGiving0,
				LambdaStrassen.leftToRightGiving1,
				LambdaStrassen.preparedPairs,
				strassenOrderedI,
				Optional.of(sortedStrassenIJKL.get(1))).parallel().count();

		Assert.assertTrue(nbSolutions > 0);
		System.out.println(nbSolutions);

		List<IJKLAndAEs> asForStrassen = Strassen
				.allIJKLAsStream(LambdaStrassen.leftToRightGiving0,
						LambdaStrassen.leftToRightGiving1,
						LambdaStrassen.preparedPairs,
						strassenOrderedI,
						Optional.of(sortedStrassenIJKL.get(1)))
				.parallel()
				.filter(sol -> sol.ijkl.k.equals(sortedStrassenIJKL.get(2))
						&& sol.ijkl.l.equals(sortedStrassenIJKL.get(3)))
				.collect(Collectors.toList());

		Assert.assertTrue(asForStrassen.size() > 0);
		System.out.println(asForStrassen);

		// https://fr.wikipedia.org/wiki/Algorithme_de_Strassen

		V5 strassenA = new V5(new int[] { 1, 0, 1, 0, 1, -1, 0 });
		V5 strassenB = new V5(new int[] { 0, 1, 0, 0, 0, 1, 0 });
		V5 strassenC = new V5(new int[] { 0, 0, 0, 0, 1, 0, 1 });

		V5 strassenE = new V5(new int[] { 1, 1, 0, -1, 0, 1, 0 });
		V5 strassenG = new V5(new int[] { 0, 0, 1, 0, 0, 1, 0 });
		V5 strassenH = new V5(new int[] { 1, 0, -1, 0, 1, 0, 1 });

		V5 strassenAE = strassenA.multiply(strassenE);
		V5 strassenBE = strassenB.multiply(strassenE);

		V5 strassenAG = strassenA.multiply(strassenG);
		V5 strassenCH = strassenC.multiply(strassenH);

		// Check IAE == 1
		Assert.assertEquals(1, unsortedStrassenIJKL.get(0).multiplyToScalar(strassenAE));

		// I in sorted is K in standard: Check KAG == 1
		Assert.assertEquals(1, unsortedStrassenIJKL.get(indexOfIinUnoredred).multiplyToScalar(strassenAG));

		// [V5 [v0=[-1, -1, 0, 1, 1, 0, 1]], V5 [v0=[0, 1, 1, -1, 0, 0, 1]], V5 [v0=[0, 0, 1, 0, 0, -1, 0]], V5 [v0=[-1,
		// -1, 1, 1, 0, -1, 0]], V5 [v0=[1, 0, 0, 0, 1, -1, 0]], V5 [v0=[0, 0, 0, 0, 1, 0, 1]], V5 [v0=[-1, 0, 1, 0, 0,
		// 0, 1]], V5 [v0=[0, -1, 0, 1, 1, -1, 0]], V5 [v0=[1, 1, 1, -1, 0, -1, 0]], V5 [v0=[1, 1, 0, -1, 1, 0, 1]]]

		int nbGoodCandidate = 0;
		for (IJKLAndAEs candidate : asForStrassen) {
			if (candidate.aeCandidates.contains(strassenAG)) {
				nbGoodCandidate++;
			}
			if (candidate.aeCandidates.contains(strassenCH)) {
				nbGoodCandidate++;
			}
		}
		Assert.assertTrue(nbGoodCandidate > 0);

		List<List<V5>> solutions = asForStrassen.stream()
				.flatMap(ijkl -> Strassen.processIJKL(LambdaStrassen.leftToRightGiving0,
						LambdaStrassen.leftToRightGiving1,
						// aeToAE,
						LambdaStrassen.preparedPairs,
						ijkl.aeCandidates,
						ijkl.ijkl))
				.collect(Collectors.toList());

		Assert.assertTrue(solutions.size() > 0);

		Set<V5> strassenABCD_EFGH = ImmutableSet.<V5>builder()
				.add(strassenA,
						strassenB,
						strassenC,
						new V5(new int[] { 1, 1, 0, 1, 0, 0, -1 }),

						strassenE,
						new V5(new int[] { 0, 0, 0, 1, 0, 0, 1 }),
						strassenG,
						strassenH)
				.build();

		Set<V5> strassenIJKLABCDEFGH =
				ImmutableSet.<V5>builder().addAll(sortedStrassenIJKL).addAll(strassenABCD_EFGH).build();

		// These are not solutions at all as we mixed IJKL with ABCD and EFGH
		Set<Set<V5>> unorderedSolutions =
				solutions.stream().map(l -> ImmutableSet.copyOf(l)).collect(Collectors.toSet());

		// This is a weak check, but still a condition which has to hold true
		Assert.assertTrue(unorderedSolutions.contains(strassenIJKLABCDEFGH));

		// List<List<V5>> count = new LambdaStrassen().countForIJKL(sortedStrassenIJKL);
		//
		// // Ensure we get ride of symmetries
		// // long countAsSet = count.stream().map(list -> {
		// // List<V5> copy = list.stream().sorted((l, r) -> r.isStrictlyAfter(l)).collect(Collectors.toList());
		// //
		// // return list;
		// // }).distinct().count();
		// // Assert.assertEquals(countAsSet, count.size());
		//
		// count.forEach(answer -> {
		// Map<String, V5> keyToVector = new LinkedHashMap<>();
		//
		// keyToVector.put("i", answer.get(0));
		// keyToVector.put("j", answer.get(1));
		// keyToVector.put("k", answer.get(2));
		// keyToVector.put("l", answer.get(3));
		//
		// keyToVector.put("a", answer.get(4));
		// keyToVector.put("b", answer.get(5));
		// keyToVector.put("c", answer.get(6));
		// keyToVector.put("d", answer.get(7));
		//
		// keyToVector.put("e", answer.get(8));
		// keyToVector.put("f", answer.get(9));
		// keyToVector.put("g", answer.get(10));
		// keyToVector.put("h", answer.get(11));
		//
		// // Strassen.FORCE_GROWING.forEach(require -> {
		// // V5 smaller = keyToVector.get(require.get(0));
		// // V5 bigger = keyToVector.get(require.get(1));
		// //
		// // if (!bigger.isStrictlyAfter(smaller)) {
		// // Assert.assertTrue(bigger.isStrictlyAfter(smaller));
		// // }
		// // });
		// });
		//
		// Assert.assertEquals(1, count.size());
	}

	@Test
	public void testVectorsGiving1() {
		Strassen.power = 5;
		SetMultimap<V5, V5> to1 = Strassen.leftToRightGiving1();

		V5 problem = new V5(new int[] { -1, -1, -1, -1, -1 });
		V5 knownSolution = new V5(new int[] { -1, 0, 0, 0, 0 });
		Assert.assertEquals(1, problem.multiplyToScalar(knownSolution));

		Set<V5> possibleRights = to1.get(problem);
		Assert.assertTrue(possibleRights.contains(knownSolution));

		Set<V5> possibleRights_Dual = to1.get(knownSolution);
		Assert.assertTrue(possibleRights_Dual.contains(problem));
	}
}
