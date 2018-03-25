package io.cormoran.strassen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.math.LongMath;

import cormoran.pepper.jvm.GCInspector;
import cormoran.pepper.logging.PepperLogHelper;
import cormoran.pepper.thread.PepperExecutorsHelper;

public class Strassen {
	public static final ImmutableSet<List<String>> FORCE_GROWING = ImmutableSet.<List<String>>builder()
			// .add(Arrays.<String>asList("a", "e")
			// // , Arrays.<String>asList("e", "g"), Arrays.<String>asList("a", "g")
			// ).add(Arrays.<String>asList("c", "f")
			// // , Arrays.<String>asList("f", "h"), Arrays.<String>asList("c", "h")
			// )
			// // Ensure second matrix of ones is strictly after the first one
			// .add(Arrays.<String>asList("a", "c"))
			// // Transitivity
			// .add(Arrays.<String>asList("a", "f"))
			.build();

	protected static final Logger LOGGER = LoggerFactory.getLogger(Strassen.class);

	public static int power = 5;

	public static void main(String[] args) throws MalformedObjectNameException, InstanceNotFoundException {
		new GCInspector().afterPropertiesSet();

		// Should be 3^5
		LOGGER.info("Count Vectors: " + all().count());

		// V5 zero = new V5(0, 0, 0, 0, 0);
		long count = newStreamOfPairs().filter(pair -> 0 == pair.get(0).multiplyToScalar(pair.get(1))).count();

		LOGGER.info("Count pair giving 0: " + count);

		long countPairsTo1 = newStreamOfPairs().filter(pair -> 1 == pair.get(0).multiplyToScalar(pair.get(1))).count();

		LOGGER.info("Count pair giving 1: " + countPairsTo1);

		SetMultimap<V5, V5> leftToRightGiving0 = leftToRightFor0();
		SetMultimap<V5, V5> leftToRightGiving1 = leftToRightGiving1();

		LOGGER.info("_2 Hash done");

		long countIJKL = allIJKLAsStream().count();

		LOGGER.info("Count IJKL: " + countIJKL);

		boolean aws = false;

		ScheduledExecutorService es = PepperExecutorsHelper.newSingleThreadScheduledExecutor("StrassenLog");
		AtomicInteger countOK = new AtomicInteger();
		final long max;
		if (aws) {
			long countIJK = allIJKLAsStream().parallel()
					.map(ijkl -> new QueryIJK(ijkl.ijkl.i, ijkl.ijkl.j, ijkl.ijkl.k))
					.distinct()
					.count();
			max = countIJK;
		} else {
			max = countIJKL;
		}
		final LongAdder progress = new LongAdder();
		es.scheduleAtFixedRate(() -> LOGGER.info("Count: {} . Progress = {} / {} ({})",
				countOK,
				progress,
				max,
				PepperLogHelper.getNicePercentage(progress.longValue(), max)), 1, 1, TimeUnit.SECONDS);

		SetMultimap<V5, List<V5>> preparedPairs = preparedPairs();

		ICormoranLambdaScrapper catService;
		if (aws) {
			AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard();
			builder.setRegion(Regions.US_EAST_1.getName());
			AWSLambda awsLambda = builder.build();

			catService = LambdaInvokerFactory.builder().lambdaClient(awsLambda).build(ICormoranLambdaScrapper.class);
		} else {
			catService = null;
		}

		Set<QueryIJK> ijkSubmitted = Sets.newConcurrentHashSet();

		long countSolutions = allIJKLAsStream(leftToRightGiving0, leftToRightGiving1, preparedPairs).parallel()
				.peek(ijkl -> progress.increment())
				.flatMap(ijklAndAEs -> {
					if (aws) {
						// One query per IJKL leads to too many queries: Network/IO/latency is dominant -> too slow
						// QueryIJK queryIJK = new QueryIJK(ijkl.get(0), ijkl.get(1), ijkl.get(2));
						QueryIJK queryIJK = new QueryIJK(ijklAndAEs.ijkl.i, ijklAndAEs.ijkl.j, ijklAndAEs.ijkl.k);

						if (ijkSubmitted.add(queryIJK)) {
							List<AnswerIJKLABCDEFGH> output = catService.countCats(queryIJK);

							return output.stream().map(answer -> {
								LOGGER.info("One solution: ijkl={} abcd={} efgh={}",
										answer.getIjkl(),
										answer.getAbcd(),
										answer.getEfgh());

								return Arrays.asList(answer.getIjkl().getI(),
										answer.getIjkl().getJ(),
										answer.getIjkl().getK(),
										answer.getIjkl().getL(),

										answer.getAbcd().getI(),
										answer.getAbcd().getJ(),
										answer.getAbcd().getK(),
										answer.getAbcd().getL(),

										answer.getEfgh().getI(),
										answer.getEfgh().getJ(),
										answer.getEfgh().getK(),
										answer.getEfgh().getL());
							});
						} else {
							// Already submitted for computation
							return Stream.empty();
						}
					} else {
						return processIJKL(leftToRightGiving0,
								leftToRightGiving1,
								preparedPairs,
								ijklAndAEs.aeCandidates,
								ijklAndAEs.ijkl);
					}
				})
				.peek(ok -> countOK.incrementAndGet())
				.count();

		LOGGER.info("Count solutions IJKL ABC E: {}", countSolutions);
	}

	@Deprecated
	private static Stream<IJKLAndAEs> allIJKLAsStream() {
		return allIJKLAsStream(leftToRightFor0(), leftToRightGiving1(), preparedPairs());
	}

	public static SetMultimap<V5, V5> leftToRightGiving1() {
		SetMultimap<V5, V5> leftToRightGiving1 = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

		newStreamOfPairs().filter(pair -> 1 == pair.get(0).multiplyToScalar(pair.get(1)))
				.forEach(pair -> leftToRightGiving1.put(pair.get(0), pair.get(1)));
		return leftToRightGiving1;
	}

	public static SetMultimap<V5, V5> leftToRightFor0() {
		SetMultimap<V5, V5> leftToRightGiving0 = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

		// We have no interest in vectors full of 0s
		V5 zero = new V5(new int[newStreamOfPairs().findFirst().get().get(0).v0.length]);

		newStreamOfPairs().filter(pair -> 0 == pair.get(0).multiplyToScalar(pair.get(1))).filter(pair ->
		// We want to receive all vectors associated to zero, but not zero as associated to another vector
		// !pair.get(0).equals(zero) &&
		!pair.get(1).equals(zero)).forEach(pair -> leftToRightGiving0.put(pair.get(0), pair.get(1)));

		return leftToRightGiving0;
	}

	public static SetMultimap<V5, List<V5>> preparedPairs() {
		SetMultimap<V5, List<V5>> aeToAE = MultimapBuilder.hashKeys().hashSetValues().build();

		// e is is strictly after a
		newStreamOfPairs()
				// .filter(listAE -> listAE.get(1).isStrictlyAfter(listAE.get(0)))
				.forEach(ae -> {
					V5 a = ae.get(0);
					V5 e = ae.get(1);
					aeToAE.put(a.multiply(e), Arrays.asList(a, e));
				});

		return aeToAE;
	}

	public static Stream<List<V5>> processIJKL(SetMultimap<V5, V5> leftToRightGiving0,
			SetMultimap<V5, V5> leftToRightGiving1,
			SetMultimap<V5, List<V5>> aeToAAndE,
			Set<V5> okAE,
			QueryIJKL ijkl) {
		V5 i = ijkl.i;
		V5 j = ijkl.j;
		V5 k = ijkl.k;
		V5 l = ijkl.l;

		V5 strassenA = new V5(new int[] { 1, 0, 1, 0, 1, -1, 0 });
		V5 strassenB = new V5(new int[] { 0, 1, 0, 0, 0, 1, 0 });
		V5 strassenC = new V5(new int[] { 0, 0, 0, 0, 1, 0, 1 });

		V5 strassenE = new V5(new int[] { 1, 1, 0, -1, 0, 1, 0 });
		V5 strassenG = new V5(new int[] { 0, 0, 1, 0, 0, 1, 0 });
		V5 strassenH = new V5(new int[] { 1, 0, -1, 0, 1, 0, 1 });

		return okAE.stream().flatMap(ae -> aeToAAndE.get(ae).stream()).flatMap(listAE -> {
			V5 a = listAE.get(0);
			V5 e = listAE.get(1);

			if (a.equals(strassenA) && e.equals(strassenG)) {
				System.out.println("a:" + a);
				System.out.println("e:" + e);
			}

			V5 ia = i.multiply(a);
			V5 ja = j.multiply(a);
			V5 ka = k.multiply(a);
			V5 la = l.multiply(a);

			// Constrain on F, G and H are stronger: higher change to be rejected early
			Set<V5> gForAGAndAF =
					intersect3(leftToRightGiving0.get(ia), leftToRightGiving0.get(ja), leftToRightGiving0.get(la))
							.stream()
							.collect(Collectors.toSet());

			Set<V5> fForKAF = leftToRightGiving0.get(ka);
			Set<V5> fForAF =
					Sets.intersection(applyOrdering("e", e, "f", applyOrdering("a", a, "f", gForAGAndAF)), fForKAF)
							.stream()
							.collect(Collectors.toSet());
			if (fForAF.isEmpty()) {
				return Stream.empty();
			}

			Set<V5> gForAG =
					Sets.intersection(gForAGAndAF, leftToRightGiving1.get(ka)).stream().collect(Collectors.toSet());
			if (gForAG.isEmpty()) {
				return Stream.empty();
			}

			Set<V5> hForAH = fForAF;

			V5 ie = i.multiply(e);
			V5 je = j.multiply(e);
			V5 ke = k.multiply(e);
			V5 le = l.multiply(e);

			Set<V5> bForIE = leftToRightGiving0.get(ie);

			Set<V5> bForJE = leftToRightGiving1.get(je);
			Set<V5> cForJE = leftToRightGiving0.get(je);

			Set<V5> bForKE = leftToRightGiving0.get(ke);
			Set<V5> bForLE = leftToRightGiving0.get(le);

			Set<V5> bForBEAndCE = intersect3(bForIE, bForKE, bForLE).stream().collect(Collectors.toSet());

			// Enforce ordering as early as possible
			Set<V5> cForCE =
					Sets.intersection(applyOrdering("e", e, "c", applyOrdering("a", a, "c", bForBEAndCE)), cForJE)
							.stream()
							.collect(Collectors.toSet());
			if (cForCE.isEmpty()) {
				return Stream.empty();
			}

			Set<V5> bForBE = Sets.intersection(bForBEAndCE, bForJE).stream().collect(Collectors.toSet());
			if (bForBE.isEmpty()) {
				return Stream.empty();
			}

			Set<V5> dForDE = cForCE;

			if (Stream.of(bForBE, cForCE, dForDE, fForAF, gForAG, hForAH)
					.filter(Collection::isEmpty)
					.findAny()
					.isPresent()) {
				return Stream.empty();
			}

			Map<String, Set<V5>> nameToValues = ImmutableMap.<String, Set<V5>>builder()
					.put("b", bForBE)
					.put("c", cForCE)
					.put("d", dForDE)
					.put("f", fForAF)
					.put("g", gForAG)
					.put("h", hForAH)
					.build()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(ee -> ee.getKey(),
							ee -> applyOrdering("a",
									a,
									ee.getKey(),
									applyOrdering("e", e, ee.getKey(), ee.getValue()))));

			Entry<String, Set<V5>> min = selectMin(nameToValues);

			LOGGER.trace("Min: {}", min);
			if (min.getValue().isEmpty()) {
				return Stream.empty();
			}

			System.out.println("Candidate: a=" + a + " e=" + e + " min:" + min);

			return toStream(ijkl, a, e, nameToValues, min);
		});
	}

	private static Set<V5> intersect3(Set<V5> set, Set<V5> set2, Set<V5> set3) {
		final Set<V5> min;
		final Set<V5> secondMin;
		final Set<V5> max;

		if (set.size() < set2.size()) {
			if (set2.size() < set3.size()) {
				min = set;
				secondMin = set2;
				max = set3;
			} else {
				max = set2;
				if (set.size() < set3.size()) {
					min = set;
					secondMin = set3;
				} else {
					min = set3;
					secondMin = set;
				}
			}
		} else {
			assert set2.size() <= set.size();
			if (set3.size() < set2.size()) {
				min = set3;
				secondMin = set2;
				max = set;
			} else {
				assert set2.size() <= set3.size();
				min = set2;
				if (set.size() < set3.size()) {
					secondMin = set;
					max = set3;
				} else {
					secondMin = set3;
					max = set;
				}
			}
		}
		Set<V5> intermediate = Sets.intersection(min, secondMin);

		return Sets.intersection(intermediate, max);
	}

	public static Stream<IJKLAndAEs> allIJKLAsStream(SetMultimap<V5, V5> leftToRightGiving0,
			SetMultimap<V5, V5> leftToRightGiving1,
			SetMultimap<V5, List<V5>> preparedPairs) {
		List<V5> orderedNotZero = orderedNotZero(leftToRightGiving0);

		// Order IJKL matrix columns: we order the first columns
		IntStream indexICandidates = IntStream.range(0, orderedNotZero.size());

		return allIJKLAsStream(leftToRightGiving0,
				leftToRightGiving1,
				orderedNotZero,
				indexICandidates,
				Optional.empty());
	}

	static List<V5> orderedNotZero(SetMultimap<V5, V5> leftToRightGiving0) {
		V5 zero = new V5(new int[leftToRightGiving0.keySet().iterator().next().v0.length]);

		// smallToBig
		List<V5> orderedNotZero = all().sorted().filter(l -> !l.equals(zero)).collect(Collectors.toList());
		return orderedNotZero;
	}

	private static Stream<IJKLAndAEs> allIJKLAsStream(SetMultimap<V5, V5> leftToRightGiving0,
			SetMultimap<V5, V5> leftToRightGiving1,
			List<V5> orderedNotZero,
			IntStream indexICandidates,
			Optional<V5> filterJ) {
		V5 zero = new V5(new int[orderedNotZero.get(0).v0.length]);

		return indexICandidates.mapToObj(i -> i)
				// J > I
				.flatMap(indexI -> IntStream.range(indexI + 1, orderedNotZero.size())
						.mapToObj(indexJ -> indexJ)
						// K > J
						.flatMap(indexJ -> {
							V5 vectorI = orderedNotZero.get(indexI);

							Set<V5> okAEForI = leftToRightGiving1.get(vectorI);

							if (okAEForI.isEmpty()) {
								System.out.println("OUCH 0");
							}

							V5 vectorJ = orderedNotZero.get(indexJ);
							if (filterJ.isPresent() && !vectorJ.equals(filterJ.get())) {
								LOGGER.trace("Rejected because {} <> {}", vectorJ, filterJ.get());
								return Stream.empty();
							}

							Set<V5> okAEForJ = leftToRightGiving0.get(vectorJ);

							if (okAEForJ.isEmpty()) {
								System.out.println("OUCH 1");
							}

							Set<V5> okAEForIJ = ImmutableSet.copyOf(Sets.intersection(okAEForI, okAEForJ));
							if (okAEForIJ.isEmpty()) {
								// System.out.println("OUCH 2");
								return Stream.empty();
							}

							return IntStream.range(indexJ + 1, orderedNotZero.size())
									.mapToObj(indexK -> indexK)
									// L > K => L > I
									.flatMap(indexK -> {

										V5 vectorK = orderedNotZero.get(indexK);

										Set<V5> okAEForK = leftToRightGiving0.get(vectorK);
										if (okAEForK.isEmpty()) {
											System.out.println("OUCH 3");
										}

										Set<V5> okAEForIJK =
												ImmutableSet.copyOf(Sets.intersection(okAEForIJ, okAEForK));
										if (okAEForIJK.isEmpty()) {
											// System.out.println("OUCH 4");
											return Stream.empty();
										}

										V5 absI;
										if (vectorI.isStrictlyAfter(zero)) {
											absI = vectorI;
										} else {
											// vectorI < 0 : absolute value is *-1
											absI = vectorI.multiplyByScalar(-1);
										}

										return IntStream.range(indexK + 1, orderedNotZero.size())

												// We also want to enforce |L| < |I| (or L < -I), else we could multiply
												// I|J|K|L
												// by -1
												.filter(indexL -> checkSymetryFromIToL(zero,
														orderedNotZero,
														absI,
														indexL))
												// We filter indexL by Sets.intersect in a second pass as it is slower
												.mapToObj(indexL -> {

													V5 vectorL = orderedNotZero.get(indexL);
													Set<V5> okAEForL = leftToRightGiving0.get(vectorL);

													if (okAEForL.isEmpty()) {
														System.out.println("OUCH 5");
													}

													Set<V5> okAE = ImmutableSet
															.copyOf(Sets.intersection(okAEForIJK, okAEForL));

													return new IJKLAndAEs(
															new QueryIJKL(orderedNotZero.get(indexI),
																	orderedNotZero.get(indexJ),
																	orderedNotZero.get(indexK),
																	orderedNotZero.get(indexL)),
															okAE);

												}

				);
									});
						}));
	}

	public static boolean checkSymetryFromIToL(V5 zero, List<V5> orderedNotZero, V5 absI, int indexL) {
		V5 vectorL = orderedNotZero.get(indexL);

		if (vectorL.isStrictlyAfter(zero)) {
			// vectorL == absL => we check L is after I
			return vectorL.isStrictlyAfter(absI);
		} else {
			// vectorL == -1*absL
			V5 absL = vectorL.multiplyByScalar(-1);

			return absL.isStrictlyAfter(absI);
		}
	}

	public static Stream<IJKLAndAEs> allIJKLAsStream(SetMultimap<V5, V5> leftToRightGiving0,
			SetMultimap<V5, V5> leftToRightGiving1,
			SetMultimap<V5, List<V5>> preparedPairs,
			V5 singleICandidate,
			Optional<V5> filterJ) {
		List<V5> orderedNotZero = orderedNotZero(leftToRightGiving0);

		// Order IJKL matrix columns: we order the first columns
		int indexOf = orderedNotZero.indexOf(singleICandidate);

		if (indexOf < 0) {
			throw new IllegalStateException("Invalid value for I: " + singleICandidate);
		}

		IntStream indexICandidates = IntStream.of(indexOf);

		return allIJKLAsStream(leftToRightGiving0, leftToRightGiving1, orderedNotZero, indexICandidates, filterJ);
	}

	private static Stream<List<V5>> toStream(QueryIJKL list,
			V5 a,
			V5 e,
			Map<String, Set<V5>> nameToValues,
			Entry<String, Set<V5>> min) {
		return min.getValue().stream().flatMap(b -> {
			Map<String, Set<V5>> nameToValues2 = reduce(list, nameToValues, min.getKey(), b);

			Entry<String, Set<V5>> min2 = selectMin(nameToValues2);

			if (min2.getValue().isEmpty()) {
				return Stream.empty();
			}

			return min2.getValue().stream().flatMap(c -> {
				Map<String, Set<V5>> nameToValues3 = reduce(list, nameToValues2, min2.getKey(), c);

				Entry<String, Set<V5>> min3 = selectMin(nameToValues3);

				if (min3.getValue().isEmpty()) {
					return Stream.empty();
				}

				return min3.getValue().stream().flatMap(d -> {
					Map<String, Set<V5>> nameToValues4 = reduce(list, nameToValues3, min3.getKey(), d);

					Entry<String, Set<V5>> min4 = selectMin(nameToValues4);

					if (min4.getValue().isEmpty()) {
						return Stream.empty();
					}

					return min4.getValue().stream().flatMap(f -> {
						Map<String, Set<V5>> nameToValues5 = reduce(list, nameToValues4, min4.getKey(), f);

						Entry<String, Set<V5>> min5 = selectMin(nameToValues5);

						if (min5.getValue().isEmpty()) {
							return Stream.empty();
						}

						return min5.getValue().stream().flatMap(g -> {
							Map<String, Set<V5>> nameToValues6 = reduce(list, nameToValues5, min5.getKey(), g);

							Entry<String, Set<V5>> min6 = selectMin(nameToValues6);

							if (min6.getValue().isEmpty()) {
								return Stream.empty();
							}

							return min6.getValue().stream().flatMap(h -> {
								LOGGER.info("One solution: a={} b={} c={} d={} e={} f={} g={} h={}",
										a,
										b,
										c,
										d,
										e,
										f,
										g,
										h);
								return Stream.of(Arrays.asList(list.i, list.j, list.k, list.l, a, b, c, d, e, f, g, h));
							});
						});
					});
				});
			});
		});
	}

	private static Entry<String, Set<V5>> selectMin(Map<String, Set<V5>> nameToValues2) {
		return nameToValues2.entrySet().stream().min(Comparator.comparing(entry -> entry.getValue().size())).get();
	}

	private static Map<String, Set<V5>> reduce(QueryIJKL ijkl, Map<String, Set<V5>> nameToValues, String minKey, V5 b) {
		return nameToValues.entrySet()
				.stream()
				.filter(ee -> !ee.getKey().equals(minKey))
				.collect(Collectors.toMap(ee -> ee.getKey(),
						ee -> applyOrdering(minKey,
								b,
								ee.getKey(),
								applyConditions(ijkl, minKey, b, ee.getKey(), ee.getValue()))));
	}

	/**
	 * Given (ae,be,ag,bg), we have multiple symmetries which can be broken by a < e (as a and e are exchangeable), e <
	 * b, a < g
	 * 
	 * @param selectedKey
	 * @param selected
	 * @param allowedKey
	 * @param allowed
	 * @return
	 */
	private static Set<V5> applyOrdering(String selectedKey, V5 selected, String allowedKey, Set<V5> allowed) {
		ImmutableList<String> coming = ImmutableList.of(selectedKey, allowedKey);

		if (FORCE_GROWING.contains(coming)) {
			return allowed.stream().filter(v5 -> v5.isStrictlyAfter(selected)).collect(Collectors.toSet());
		} else if (FORCE_GROWING.contains(coming.reverse())) {
			return allowed.stream().filter(v5 -> selected.isStrictlyAfter(v5)).collect(Collectors.toSet());
		} else {
			return allowed;
		}
	}

	private static Set<String> ABCD = ImmutableSet.of("a", "b", "c", "d");
	private static Set<String> EFGH = ImmutableSet.of("e", "f", "g", "h");

	private static final Set<Set<String>> bgdh = ImmutableSet.of(ImmutableSet.of("b", "g"), ImmutableSet.of("d", "h"));
	private static final Set<Set<String>> agch = ImmutableSet.of(ImmutableSet.of("a", "g"), ImmutableSet.of("c", "h"));
	private static final Set<Set<String>> bedf = ImmutableSet.of(ImmutableSet.of("b", "e"), ImmutableSet.of("d", "f"));
	private static final Set<Set<String>> aecf = ImmutableSet.of(ImmutableSet.of("a", "e"), ImmutableSet.of("c", "f"));

	private static Set<V5> applyConditions(QueryIJKL ijkl,
			String valueName,
			V5 valueEnforced,
			String candidateColumn,
			Set<V5> leftCandidates) {
		if (ABCD.contains(valueName) && ABCD.contains(candidateColumn)) {
			return leftCandidates;
		} else if (EFGH.contains(valueName) && EFGH.contains(candidateColumn)) {
			return leftCandidates;
		} else {
			Set<String> question = ImmutableSet.of(valueName, candidateColumn);
			if (aecf.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.i) == 1 && restricted.multiplyToScalar(ijkl.j) == 0
							&& restricted.multiplyToScalar(ijkl.k) == 0
							&& restricted.multiplyToScalar(ijkl.l) == 0;
				}).collect(Collectors.toSet());
			} else if (bedf.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.i) == 0 && restricted.multiplyToScalar(ijkl.j) == 1
							&& restricted.multiplyToScalar(ijkl.k) == 0
							&& restricted.multiplyToScalar(ijkl.l) == 0;
				}).collect(Collectors.toSet());
			} else if (agch.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.i) == 0 && restricted.multiplyToScalar(ijkl.j) == 0
							&& restricted.multiplyToScalar(ijkl.k) == 1
							&& restricted.multiplyToScalar(ijkl.l) == 0;
				}).collect(Collectors.toSet());
			} else if (bgdh.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.i) == 0 && restricted.multiplyToScalar(ijkl.j) == 0
							&& restricted.multiplyToScalar(ijkl.k) == 0
							&& restricted.multiplyToScalar(ijkl.l) == 1;
				}).collect(Collectors.toSet());
			} else {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.i) == 0 && restricted.multiplyToScalar(ijkl.j) == 0
							&& restricted.multiplyToScalar(ijkl.k) == 0
							&& restricted.multiplyToScalar(ijkl.l) == 0;
				}).collect(Collectors.toSet());
			}

		}
	}

	private static Stream<List<V5>> newStreamOfPairs() {
		return newStreamOfNlets(2);
	}

	private static Stream<List<V5>> newStreamOfNlets(int n) {
		List<Set<V5>> asList = new ArrayList<>();

		IntStream.range(0, n).forEach(i -> asList.add(all().map(v5 -> v5).collect(Collectors.toSet())));

		return Sets.cartesianProduct(asList).stream().map(l -> ImmutableList.copyOf(l));
	}

	private static Stream<V5> all() {
		// -1, 0, 1 -> 3 values
		int minValue = -1;
		int maxValue = 1;
		// +1 for 0
		int nbValues = maxValue - minValue + 1;
		final long limit = LongMath.checkedPow(nbValues, power);

		// First value: min values everywhere
		int[] array = IntStream.range(0, power).map(i -> minValue).toArray();

		// TODO: Check the stream is sorted
		return Stream.iterate(new V5(array), v5 -> v5.increment(minValue, maxValue)).limit(limit);
	}
}
