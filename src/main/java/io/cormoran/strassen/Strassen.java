package io.cormoran.strassen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import cormoran.pepper.logging.PepperLogHelper;

public class Strassen {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Strassen.class);

	public static void main(String[] args) {
		Set<Block> singleA = new LinkedHashSet<>();
		Set<Block> singleB = new LinkedHashSet<>();
		int nbColumns = 2;
		int nbShared = 2;
		int nbRows = 2;

		// Consider all possible single blocks
		Sets.cartesianProduct(new HashSet<>(Ints.asList(IntStream.range(0, nbShared).toArray())),
				new HashSet<>(Ints.asList(IntStream.range(0, nbColumns).toArray())))
				.forEach(coordinate -> singleA.add(new Block(coordinate.get(0), coordinate.get(1))));

		Sets.cartesianProduct(new HashSet<>(Ints.asList(IntStream.range(0, nbRows).toArray())),
				new HashSet<>(Ints.asList(IntStream.range(0, nbShared).toArray())))
				.forEach(coordinate -> singleB.add(new Block(coordinate.get(0), coordinate.get(1))));

		Set<Set<Block>> aBlocks = new HashSet<>();
		Set<Set<Block>> bBlocks = new HashSet<>();

		aBlocks.add(Collections.emptySet());
		bBlocks.add(Collections.emptySet());

		// Compute all combinations of blocks sums
		for (int i = 2; i <= singleA.size(); i++) {
			singleA.forEach(coordinate -> {
				// A group may hold from a single block to all blocks
				aBlocks.addAll(aBlocks.stream().map(blockSet -> {
					Set<Block> withOneMore = new HashSet<>(blockSet);

					withOneMore.add(coordinate);

					return withOneMore;
				}).collect(Collectors.toList()));
			});
		}

		for (int i = 2; i <= singleB.size(); i++) {
			singleB.forEach(coordinate -> {
				// A group may hold from a single block to all blocks
				bBlocks.addAll(bBlocks.stream().map(blockSet -> {
					Set<Block> withOneMore = new HashSet<>(blockSet);

					withOneMore.add(coordinate);

					return withOneMore;
				}).collect(Collectors.toList()));
			});
		}

		aBlocks.remove(Collections.emptySet());
		bBlocks.remove(Collections.emptySet());

		System.out.println("aBlocks: " + aBlocks.size());
		System.out.println("bBlocks: " + bBlocks.size());

		List<List<Set<Block>>> multiplications = new ArrayList<>(Sets.cartesianProduct(aBlocks, bBlocks));

		System.out.println(multiplications.size());

		// We try to find a combination involving only 4 multiplications
		long problemSize = (long) multiplications.size() * multiplications.size()
				* multiplications.size()
				* multiplications.size();

		System.out.println(problemSize);

		// https://fr.wikipedia.org/wiki/Algorithme_de_Strassen
		Set<PairOfBLock> requiredPairs = ImmutableSet.of(new PairOfBLock(new Block(0, 0), new Block(0, 0)),
				new PairOfBLock(new Block(0, 1), new Block(1, 0)),
				new PairOfBLock(new Block(0, 0), new Block(0, 1)),
				new PairOfBLock(new Block(0, 1), new Block(1, 1)),
				new PairOfBLock(new Block(1, 0), new Block(0, 0)),
				new PairOfBLock(new Block(1, 1), new Block(1, 0)),
				new PairOfBLock(new Block(1, 0), new Block(0, 1)),
				new PairOfBLock(new Block(1, 1), new Block(1, 1)));

		AtomicLong minBlockInLast = new AtomicLong(Long.MAX_VALUE);
		AtomicLong nbCombinationWIthMin = new AtomicLong();

		AtomicLong minProcessed = new AtomicLong();
		AtomicLong nbWithCombinations = new AtomicLong();

		// Compute all possible combinations of multiplications
		for (int i = 0; i < multiplications.size(); i++) {
			System.out.println(PepperLogHelper.getNicePercentage(i, multiplications.size()));

			List<Set<Block>> first = multiplications.get(i);

			Set<Block> firstSumA = first.get(0);
			Set<Block> firstSumB = first.get(1);
			Set<PairOfBLock> firstPairs = Sets.cartesianProduct(firstSumA, firstSumB)
					.stream()
					.map(l -> new PairOfBLock(l.get(0), l.get(1)))
					.collect(Collectors.toSet());

			for (int j = i + 1; j < multiplications.size(); j++) {

				List<Set<Block>> second = multiplications.get(j);

				Set<Block> secondSumA = second.get(0);
				Set<Block> secondSumB = second.get(1);
				Set<PairOfBLock> secondPairs = Sets.cartesianProduct(secondSumA, secondSumB)
						.stream()
						.map(l -> new PairOfBLock(l.get(0), l.get(1)))
						.collect(Collectors.toSet());

				for (int k = j + 1; k < multiplications.size(); k++) {
					List<Set<Block>> third = multiplications.get(k);

					Set<Block> thirdSumA = third.get(0);
					Set<Block> thirdSumB = third.get(1);
					Set<PairOfBLock> thirdPairs = Sets.cartesianProduct(thirdSumA, thirdSumB)
							.stream()
							.map(l -> new PairOfBLock(l.get(0), l.get(1)))
							.collect(Collectors.toSet());

					IntStream.range(k + 1, multiplications.size()).parallel().unordered().forEach(l -> {

						List<Set<Block>> fourth = multiplications.get(l);

						Set<Block> fourthSumA = fourth.get(0);
						Set<Block> fourthSumB = fourth.get(1);
						Set<PairOfBLock> fourthPairs = Sets.cartesianProduct(fourthSumA, fourthSumB)
								.stream()
								.map(ll -> new PairOfBLock(ll.get(0), ll.get(1)))
								.collect(Collectors.toSet());

						Set<PairOfBLock> allCurrentPairs = new HashSet<>();
						allCurrentPairs.addAll(firstPairs);
						allCurrentPairs.addAll(secondPairs);
						allCurrentPairs.addAll(thirdPairs);
						allCurrentPairs.addAll(fourthPairs);

						if (allCurrentPairs.containsAll(requiredPairs)) {
							nbWithCombinations.incrementAndGet();
							if (false) {
								System.out.println(firstSumA + "*"
										+ firstSumB
										+ " + "
										+ secondSumA
										+ "*"
										+ secondSumB
										+ " + "
										+ thirdSumA
										+ "*"
										+ thirdSumB
										+ " + "
										+ fourthSumA
										+ "*"
										+ fourthSumB);
							}

							LinearSum sum = allCurrentPairs.stream().map(ll -> new LinearSum().add(1, ll)).collect(
									Collectors.reducing(new LinearSum(), (left, right) -> left.merge(right)));

							List<LinearSum> linearEquations = new ArrayList<>();

							// These are our 4 constrain: we search coefficients so that we find back the expected
							// multiplication
							linearEquations.add(sum.add(-1, new PairOfBLock(new Block(0, 0), new Block(0, 0))).add(-1,
									new PairOfBLock(new Block(0, 1), new Block(1, 0))));
							linearEquations.add(sum.add(-1, new PairOfBLock(new Block(0, 0), new Block(0, 1))).add(-1,
									new PairOfBLock(new Block(0, 1), new Block(1, 1))));
							linearEquations.add(sum.add(-1, new PairOfBLock(new Block(1, 0), new Block(0, 0))).add(-1,
									new PairOfBLock(new Block(1, 1), new Block(1, 0))));
							linearEquations.add(sum.add(-1, new PairOfBLock(new Block(1, 0), new Block(0, 1))).add(-1,
									new PairOfBLock(new Block(1, 1), new Block(1, 1))));

							// System.out.println("Before simplication");
							// System.out.println(linearEquations.get(0));
							// System.out.println(linearEquations.get(1));
							// System.out.println(linearEquations.get(2));
							// System.out.println(linearEquations.get(3));

							for (int eqIndex = 0; eqIndex < linearEquations.size(); eqIndex++) {
								// Select the next not-0 coefficient
								Optional<Map.Entry<PairOfBLock, Long>> max =
										linearEquations.get(eqIndex).pairWithCoef.asMap()
												.entrySet()
												.stream()
												.filter(e -> e.getValue().longValue() != 0)
												.max(Comparator.comparing(e -> e.getKey()));

								// Entry<PairOfBLock, Long> pivot =
								// linearEquations.get(0).pairWithCoef.asMap().entrySet().iterator().next();

								if (!max.isPresent()) {
									break;
								}

								PairOfBLock pairToRemove = max.get().getKey();
								long scoreToRemove = max.get().getValue();

								for (int otherEqIndex = eqIndex + 1; otherEqIndex < linearEquations
										.size(); otherEqIndex++) {
									LinearSum equation = linearEquations.get(otherEqIndex);
									long score = equation.pairWithCoef.get(pairToRemove);

									if (score != 0) {
										if (score != scoreToRemove) {
											// Integers may not be divisible: score by multiplication
											equation.scale(scoreToRemove);
											linearEquations.get(0).scale(score);
										}

										equation.mutateMinus(linearEquations.get(0));
									}
								}
							}

							if (linearEquations.get(3).pairWithCoef.size() < minBlockInLast.get()) {
								nbCombinationWIthMin.set(1);
								minBlockInLast.set(linearEquations.get(3).pairWithCoef.size());
								LOGGER.info("New min is {}", minBlockInLast);

								System.out.println("After simplication");
								System.out.println(linearEquations.get(0));
								System.out.println(linearEquations.get(1));
								System.out.println(linearEquations.get(2));
								System.out.println(linearEquations.get(3));
							} else if (linearEquations.get(3).pairWithCoef.size() == minBlockInLast.get()) {
								nbCombinationWIthMin.incrementAndGet();
							}
						}

						// Now, we need to check if there is a combination of all given products which gives the
						// expected output (i.e. the matrix multiplication)

						minProcessed.incrementAndGet();
					});
				}
			}
		}

		System.out.println("Nb combinations: " + nbWithCombinations.get());
		System.out.println("Nb combinations with min:" + nbCombinationWIthMin);
	}
}
