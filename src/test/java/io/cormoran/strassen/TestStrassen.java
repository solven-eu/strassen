package io.cormoran.strassen;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

public class TestStrassen {
	protected static final Logger LOGGER = LoggerFactory.getLogger(TestStrassen.class);

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

		List<V5> unsorted_pqrst = IntStream.range(0, Strassen.power).mapToObj(i -> {
			return new V5(new int[] { strassenIJKL.i.v0[i],
					strassenIJKL.j.v0[i],
					strassenIJKL.k.v0[i],
					strassenIJKL.l.v0[i] });
		}).collect(Collectors.toList());

		List<V5> sorted_pqrst = unsorted_pqrst.stream().sorted().collect(Collectors.toList());

		int[] sortIndexes = IntStream.range(0, Strassen.power)
				.map(index -> sorted_pqrst.indexOf(unsorted_pqrst.get(index)))
				.toArray();

		Assert.assertSame(sorted_pqrst.get(sortIndexes[0]), unsorted_pqrst.get(0));

		V5 sortedI = new V5(sorted_pqrst.stream().mapToInt(v5 -> v5.v0[0]).toArray());
		V5 sortedJ = new V5(sorted_pqrst.stream().mapToInt(v5 -> v5.v0[1]).toArray());
		V5 sortedK = new V5(sorted_pqrst.stream().mapToInt(v5 -> v5.v0[2]).toArray());
		V5 sortedL = new V5(sorted_pqrst.stream().mapToInt(v5 -> v5.v0[3]).toArray());

		LOGGER.info("sorted I: {}", sortedI);
		// [v0=[-1, 0, 0, 0, 1, 1, 1]]

		LOGGER.info("sorted J: {}", sortedJ);
		// [v0=[0, 0, 0, 1, 0, 0, 1]]

		LOGGER.info("sorted K: {}", sortedK);
		// [v0=[1, 0, 1, 0, 0, 0, 0]]

		LOGGER.info("sorted L: {}", sortedL);
		// [v0=[0, 1, 1, -1, 0, 1, 0]]

		// Assert.assertTrue(Strassen.generateAfter(sortedI, 0).collect(Collectors.toList()).contains(sortedJ));
		// Assert.assertTrue(Strassen.generateAfter(sortedJ, 0).collect(Collectors.toList()).contains(sortedK));
		// Assert.assertTrue(Strassen.generateAfter(sortedK, 0).collect(Collectors.toList()).contains(sortedL));

		Stream<IJKLAndAEs> towardsSolution = Strassen.allIJKLAsStream(LambdaStrassen.leftToRightGiving0,
				LambdaStrassen.leftToRightGiving1,
				Stream.of(sortedI),
				Optional.of(sortedJ),
				Optional.of(sortedK));

		List<IJKLAndAEs> towardsSolutionAsList = towardsSolution.parallel().collect(Collectors.toList());
		Assert.assertTrue(towardsSolutionAsList.size() > 0);

		towardsSolutionAsList.forEach(this::check);

		List<List<V5>> solutions = towardsSolutionAsList.stream()
				.flatMap(ijklAndAEs -> Strassen.processIJKL(LambdaStrassen.leftToRightGiving0,
						LambdaStrassen.leftToRightGiving1,
						LambdaStrassen.preparedPairs,
						ijklAndAEs.aeCandidates,
						ijklAndAEs.ijkl))
				.parallel()
				.collect(Collectors.toList());

		Assert.assertTrue(solutions.size() > 0);

		// {
		// V5 strassenOrderedL = sortedStrassenIJKL.get(3);
		// V5 zero = new V5(new int[strassenOrderedL.v0.length]);
		//
		// Assert.assertTrue("I is not positive", strassenOrderedI.isStrictlyAfter(zero));
		//
		// Assert.assertTrue(Strassen.checkSymetryFromIToL(zero,
		// orderedNotZero,
		// strassenOrderedI,
		// orderedNotZero.indexOf(strassenOrderedL)));
		// }
		//
		// long nbSolutions = Strassen.allIJKLAsStream(LambdaStrassen.leftToRightGiving0,
		// LambdaStrassen.leftToRightGiving1,
		// LambdaStrassen.preparedPairs,
		// strassenOrderedI,
		// Optional.of(sortedStrassenIJKL.get(1))).parallel().count();
		//
		// Assert.assertTrue(nbSolutions > 0);
		// System.out.println(nbSolutions);
		//
		// List<IJKLAndAEs> asForStrassen = Strassen
		// .allIJKLAsStream(LambdaStrassen.leftToRightGiving0,
		// LambdaStrassen.leftToRightGiving1,
		// LambdaStrassen.preparedPairs,
		// strassenOrderedI,
		// Optional.of(sortedStrassenIJKL.get(1)))
		// .parallel()
		// .filter(sol -> sol.ijkl.k.equals(sortedStrassenIJKL.get(2))
		// && sol.ijkl.l.equals(sortedStrassenIJKL.get(3)))
		// .collect(Collectors.toList());

		// Assert.assertTrue(asForStrassen.size() > 0);
		// System.out.println(asForStrassen);

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
		Assert.assertEquals(1, strassenIJKL.i.multiplyToScalar(strassenAE));

		// I in sorted is K in standard: Check KAG == 1
		// Assert.assertEquals(1, unsortedStrassenIJKL.get(indexOfIinUnoredred).multiplyToScalar(strassenAG));
		//
		// // [V5 [v0=[-1, -1, 0, 1, 1, 0, 1]], V5 [v0=[0, 1, 1, -1, 0, 0, 1]], V5 [v0=[0, 0, 1, 0, 0, -1, 0]], V5
		// [v0=[-1,
		// // -1, 1, 1, 0, -1, 0]], V5 [v0=[1, 0, 0, 0, 1, -1, 0]], V5 [v0=[0, 0, 0, 0, 1, 0, 1]], V5 [v0=[-1, 0, 1, 0,
		// 0,
		// // 0, 1]], V5 [v0=[0, -1, 0, 1, 1, -1, 0]], V5 [v0=[1, 1, 1, -1, 0, -1, 0]], V5 [v0=[1, 1, 0, -1, 1, 0, 1]]]
		//
		// int nbGoodCandidate = 0;
		// for (IJKLAndAEs candidate : asForStrassen) {
		// if (candidate.aeCandidates.contains(strassenAG)) {
		// nbGoodCandidate++;
		// }
		// if (candidate.aeCandidates.contains(strassenCH)) {
		// nbGoodCandidate++;
		// }
		// }
		// Assert.assertTrue(nbGoodCandidate > 0);
		//
		// List<List<V5>> solutions = asForStrassen.stream()
		// .flatMap(ijkl -> Strassen.processIJKL(LambdaStrassen.leftToRightGiving0,
		// LambdaStrassen.leftToRightGiving1,
		// // aeToAE,
		// LambdaStrassen.preparedPairs,
		// ijkl.aeCandidates,
		// ijkl.ijkl))
		// .collect(Collectors.toList());
		//
		// Assert.assertTrue(solutions.size() > 0);

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

		// Set<V5> strassenIJKLABCDEFGH =
		// ImmutableSet.<V5>builder().addAll(sortedStrassenIJKL).addAll(strassenABCD_EFGH).build();
		//
		// // These are not solutions at all as we mixed IJKL with ABCD and EFGH
		// Set<Set<V5>> unorderedSolutions =
		// solutions.stream().map(l -> ImmutableSet.copyOf(l)).collect(Collectors.toSet());
		//
		// // This is a weak check, but still a condition which has to hold true
		// Assert.assertTrue(unorderedSolutions.contains(strassenIJKLABCDEFGH));

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

	private void check(IJKLAndAEs ijklAndAEs) {
		// A*E are also candidates for C*F

		// We know IJKL*A*F == 0 => IJKL * A*E*C*F == 0

		int nbKO = 0;
		for (V5 ae : ijklAndAEs.aeCandidates) {
			boolean oneCF_ok = false;
			for (V5 cf : ijklAndAEs.aeCandidates) {
				boolean notZero =
						Arrays.asList(ijklAndAEs.ijkl.i, ijklAndAEs.ijkl.j, ijklAndAEs.ijkl.k, ijklAndAEs.ijkl.l)
								.stream()
								.filter(ijkl -> 0 != ijkl.multiply(ae).multiplyToScalar(cf))
								.findAny()
								.isPresent();

				if (notZero) {
					// Search for another CF which multiplied with AE and IJKL gives 0
					continue;
				} else {
					// This CF is OK: This AE is eligible
					oneCF_ok = true;
					break;
				}
			}

			if (!oneCF_ok) {
				System.out.println("!");
				nbKO++;
			}
		}

		if (nbKO >= 1) {
			System.out.println(nbKO + " amongst " + ijklAndAEs.aeCandidates.size());
		}
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
