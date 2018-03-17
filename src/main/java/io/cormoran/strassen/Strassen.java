package io.cormoran.strassen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import cormoran.pepper.jvm.GCInspector;
import cormoran.pepper.logging.PepperLogHelper;
import cormoran.pepper.thread.PepperExecutorsHelper;

public class Strassen {
	public static final ImmutableSet<List<String>> FORCE_GROWING =
			ImmutableSet.<List<String>>builder().add(Arrays.<String>asList("a", "e")
			// , Arrays.<String>asList("e", "g"), Arrays.<String>asList("a", "g")
			).add(Arrays.<String>asList("c", "f")
			// , Arrays.<String>asList("f", "h"), Arrays.<String>asList("c", "h")
			)
					// Ensure second matrix of ones is strictly after the first one
					.add(Arrays.<String>asList("a", "c"))
					// Transitivity
					.add(Arrays.<String>asList("a", "f"))
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
					.map(ijkl -> new QueryIJK(ijkl.get(0), ijkl.get(1), ijkl.get(2)))
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

		Table<V5, V5, V5> aeToAE = aeToAE();

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

		long countSolutions = allIJKLAsStream().parallel().peek(ijkl -> progress.increment()).flatMap(ijkl -> {
			if (aws) {
				// One query per IJKL leads to too many queries: Network/IO/latency is dominant -> too slow
				QueryIJK queryIJK = new QueryIJK(ijkl.get(0), ijkl.get(1), ijkl.get(2));

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
				return processIJKL(leftToRightGiving0, leftToRightGiving1
				// , aeToAE
				, preparedPairs, ijkl);
			}
		}).peek(ok -> countOK.incrementAndGet()).count();

		LOGGER.info("Count solutions IJKL ABC E: {}", countSolutions);
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
		V5 zero = new V5(new int[power]);

		newStreamOfPairs().filter(pair -> 0 == pair.get(0).multiplyToScalar(pair.get(1)))
				.filter(pair -> !pair.get(0).equals(zero) && !pair.get(1).equals(zero))
				.forEach(pair -> leftToRightGiving0.put(pair.get(0), pair.get(1)));

		return leftToRightGiving0;
	}

	public static Table<V5, V5, V5> aeToAE() {
		Table<V5, V5, V5> aeToAE = HashBasedTable.create();

		all().forEach(a -> all().forEach(e -> {
			aeToAE.put(a, e, a.multiply(e));
		}));
		return aeToAE;
	}

	public static SetMultimap<V5, List<V5>> preparedPairs() {
		SetMultimap<V5, List<V5>> aeToAE = MultimapBuilder.hashKeys().hashSetValues().build();

		// e is is strictly after a
		newStreamOfPairs().filter(listAE -> listAE.get(1).isStrictlyAfter(listAE.get(0))).forEach(ae -> {
			V5 a = ae.get(0);
			V5 e = ae.get(1);
			aeToAE.put(a.multiply(e), Arrays.asList(a, e));
		});

		return aeToAE;
	}

	public static Stream<List<V5>> processIJKL(SetMultimap<V5, V5> leftToRightGiving0,
			SetMultimap<V5, V5> leftToRightGiving1,
			// Table<V5, V5, V5> aeToAE,
			SetMultimap<V5, List<V5>> aeToAAndE,
			List<V5> ijkl) {
		V5 i = ijkl.get(0);
		V5 j = ijkl.get(1);
		V5 k = ijkl.get(2);
		V5 l = ijkl.get(3);

		Set<V5> okAEForI = leftToRightGiving1.get(i);
		Set<V5> okAEForJ = leftToRightGiving0.get(j);
		Set<V5> okAEForK = leftToRightGiving0.get(k);
		Set<V5> okAEForL = leftToRightGiving0.get(l);

		Set<V5> okAE = Sets.intersection(intersect3(okAEForI, okAEForJ, okAEForK), okAEForL);

		return okAE.stream()
				.flatMap(ae -> aeToAAndE.get(ae).stream())
				// .filter(listAE -> listAE.getValue().multiplyToScalar(i) == 1
				// && listAE.getValue().multiplyToScalar(j) == 0
				// && listAE.getValue().multiplyToScalar(k) == 0
				// && listAE.getValue().multiplyToScalar(l) == 0)
				// .filter(listAE -> okAE.contains(listAE.getValue()))
				.flatMap(listAE -> {
					V5 a = listAE.get(0);
					V5 e = listAE.get(1);

					V5 ia = i.multiply(a);
					V5 ja = j.multiply(a);
					V5 ka = k.multiply(a);
					V5 la = l.multiply(a);

					// Constrain on F, G and H are stronger: higher change to be rejected early
					Set<V5> gForAGAndAF = intersect3(leftToRightGiving0.get(ia),
							leftToRightGiving0.get(ja),
							leftToRightGiving0.get(la)).stream().collect(Collectors.toSet());

					Set<V5> fForKAF = leftToRightGiving0.get(ka);
					Set<V5> fForAF = Sets
							.intersection(applyOrdering("e", e, "f", applyOrdering("a", a, "f", gForAGAndAF)), fForKAF)
							.stream()
							.collect(Collectors.toSet());
					if (fForAF.isEmpty()) {
						return Stream.empty();
					}

					Set<V5> gForAG = Sets.intersection(gForAGAndAF, leftToRightGiving1.get(ka)).stream().collect(
							Collectors.toSet());
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
					Set<V5> cForCE = Sets
							.intersection(applyOrdering("e", e, "c", applyOrdering("a", a, "c", bForBEAndCE)), cForJE)
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

	public static Stream<List<V5>> allIJKLAsStream() {
		V5 zero = new V5(new int[power]);

		List<V5> orderedNotZero = all().sorted((l, r) -> {
			if (l.isStrictlyAfter(r)) {
				return 1;
			} else if (l.equals(r)) {
				return 0;
			} else {
				return -1;
			}
		}).filter(l -> !l.equals(zero)).collect(Collectors.toList());

		// Order IJKL matrix columns: we order the first columns
		return IntStream.range(0, orderedNotZero.size())
				.mapToObj(i -> i)
				.flatMap(indexI -> IntStream.range(indexI + 1, orderedNotZero.size())
						.mapToObj(i -> i)
						.flatMap(indexJ -> IntStream.range(indexJ + 1, orderedNotZero.size())
								.mapToObj(i -> i)
								.flatMap(indexK -> IntStream.range(indexK + 1, orderedNotZero.size())
										.mapToObj(indexL -> Arrays.asList(orderedNotZero.get(indexI),
												orderedNotZero.get(indexJ),
												orderedNotZero.get(indexK),
												orderedNotZero.get(indexL))

		))));
	}

	public static Stream<List<V5>> allIJKLAsStream(V5 i, V5 j, V5 k) {
		V5 zero = new V5(new int[i.v0.length]);

		if (false) {
			assert j.isStrictlyAfter(i);
			assert k.isStrictlyAfter(j);
		}

		return all().filter(l -> !l.equals(zero)).filter(l -> l.isStrictlyAfter(k)).map(l -> Arrays.asList(i, j, k, l));
	}

	private static Stream<List<V5>> toStream(List<V5> list,
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
								return Stream.of(Arrays.<V5>asList(list
										.get(0), list.get(1), list.get(2), list.get(3), a, b, c, d, e, f, g, h));
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

	private static Map<String, Set<V5>> reduce(List<V5> ijkl, Map<String, Set<V5>> nameToValues, String minKey, V5 b) {
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

	private static Set<V5> applyConditions(List<V5> ijkl,
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
					return restricted.multiplyToScalar(ijkl.get(0)) == 1
							&& restricted.multiplyToScalar(ijkl.get(1)) == 0
							&& restricted.multiplyToScalar(ijkl.get(2)) == 0
							&& restricted.multiplyToScalar(ijkl.get(3)) == 0;
				}).collect(Collectors.toSet());
			} else if (bedf.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.get(0)) == 0
							&& restricted.multiplyToScalar(ijkl.get(1)) == 1
							&& restricted.multiplyToScalar(ijkl.get(2)) == 0
							&& restricted.multiplyToScalar(ijkl.get(3)) == 0;
				}).collect(Collectors.toSet());
			} else if (agch.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.get(0)) == 0
							&& restricted.multiplyToScalar(ijkl.get(1)) == 0
							&& restricted.multiplyToScalar(ijkl.get(2)) == 1
							&& restricted.multiplyToScalar(ijkl.get(3)) == 0;
				}).collect(Collectors.toSet());
			} else if (bgdh.contains(question)) {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.get(0)) == 0
							&& restricted.multiplyToScalar(ijkl.get(1)) == 0
							&& restricted.multiplyToScalar(ijkl.get(2)) == 0
							&& restricted.multiplyToScalar(ijkl.get(3)) == 1;
				}).collect(Collectors.toSet());
			} else {
				return leftCandidates.stream().filter(candidate -> {
					V5 restricted = candidate.multiply(valueEnforced);
					return restricted.multiplyToScalar(ijkl.get(0)) == 0
							&& restricted.multiplyToScalar(ijkl.get(1)) == 0
							&& restricted.multiplyToScalar(ijkl.get(2)) == 0
							&& restricted.multiplyToScalar(ijkl.get(3)) == 0;
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
		int limit = 1;
		for (int i = 0; i < power; i++) {
			// -1, 0, 1 -> 3 values
			limit *= 3;
		}

		int[] array = IntStream.range(0, power).map(i -> -1).toArray();

		return Stream.iterate(new V5(array), v5 -> v5.increment(-1, 1)).limit(limit);
	}
}
