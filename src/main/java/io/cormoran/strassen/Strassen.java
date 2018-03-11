package io.cormoran.strassen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
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
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import cormoran.pepper.logging.PepperLogHelper;
import cormoran.pepper.thread.PepperExecutorsHelper;

public class Strassen {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Strassen.class);

	public static void main(String[] args) {
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
		// List<List<V5>> allIJKL = allIJKLAsStream().collect(Collectors.toList());

		// List<List<V5>> allIJ = all().filter(i -> i.isGrowing())
		// .flatMap(i -> all().filter(j -> j.isGrowing() && j.isStrictlyAfter(i)).map(j -> Arrays.asList(i, j)))
		// .collect(Collectors.toList());
		//
		// allIJ.forEach(list -> System.out.println(list));

		ScheduledExecutorService es = PepperExecutorsHelper.newSingleThreadScheduledExecutor("StrassenLog");
		AtomicInteger countOK = new AtomicInteger();
		final long max = countIJKL;
		final LongAdder progress = new LongAdder();
		es.scheduleAtFixedRate(() -> LOGGER.info("Count: {} . Progress = {} / {} ({})",
				countOK,
				progress,
				max,
				PepperLogHelper.getNicePercentage(progress.longValue(), max)), 1, 1, TimeUnit.SECONDS);

		Table<V5, V5, V5> aeToAE = aeToAE();

		List<List<V5>> preparedPairs = preparedPairs();

		boolean aws = true;

		ICormoranLambdaScrapper catService;
		if (aws) {
			AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard();
			builder.setRegion(Regions.US_EAST_1.getName());
			AWSLambda awsLambda = builder.build();

			catService = LambdaInvokerFactory.builder().lambdaClient(awsLambda).build(ICormoranLambdaScrapper.class);
		} else {
			catService = null;
		}

		long countSolutions = allIJKLAsStream().parallel().flatMap(list -> {
			if (aws) {
				Map<?, ?> output =
						catService.countCats(new QueryIJKL(list.get(0), list.get(1), list.get(2), list.get(3)));

				Number counter = (Number) output.get("count");
				return IntStream.range(0, counter.intValue()).mapToObj(i -> i);
			} else {
				return processIJKL(leftToRightGiving0, leftToRightGiving1, aeToAE, preparedPairs, list);
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

		newStreamOfPairs().filter(pair -> 0 == pair.get(0).multiplyToScalar(pair.get(1)))
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

	public static List<List<V5>> preparedPairs() {
		return newStreamOfPairs().collect(Collectors.toList());
	}

	public static Stream<?> processIJKL(SetMultimap<V5, V5> leftToRightGiving0,
			SetMultimap<V5, V5> leftToRightGiving1,
			Table<V5, V5, V5> aeToAE,
			List<List<V5>> preparedPairs,
			List<V5> ijkl) {
		V5 i = ijkl.get(0);
		V5 j = ijkl.get(1);
		V5 k = ijkl.get(2);
		V5 l = ijkl.get(3);

		return preparedPairs.stream().filter(listAE -> listAE.get(0).isStrictlyAfter(listAE.get(1))).filter(listAE -> {
			V5 a = listAE.get(0);
			V5 e = listAE.get(1);
			// a.multiply(e)
			V5 ae = aeToAE.get(a, e);

			return ae.multiplyToScalar(i) == 1 && ae.multiplyToScalar(j) == 0
					&& ae.multiplyToScalar(k) == 0
					&& ae.multiplyToScalar(l) == 0;
		}).flatMap(listAE -> {
			V5 a = listAE.get(0);
			V5 e = listAE.get(1);

			V5 ie = i.multiply(e);
			V5 je = j.multiply(e);
			V5 ke = k.multiply(e);
			V5 le = l.multiply(e);

			V5 ia = i.multiply(a);
			V5 ja = j.multiply(a);
			V5 ka = k.multiply(a);
			V5 la = l.multiply(a);

			Set<V5> bForIE = leftToRightGiving0.get(ie);

			Set<V5> bForJE = leftToRightGiving1.get(je);
			Set<V5> cForJE = leftToRightGiving0.get(je);

			Set<V5> bForKE = leftToRightGiving0.get(ke);

			Set<V5> bForLE = leftToRightGiving0.get(le);

			Set<V5> bForBEAndCE = new HashSet<>(intersect3(bForIE, bForKE, bForLE));

			Set<V5> bForBE = new HashSet<>(Sets.intersection(bForBEAndCE, bForJE));

			// Set<V5> cForIE = leftToRightGiving0.get(ie);
			// Set<V5> cForKE = leftToRightGiving0.get(ke);
			// Set<V5> cForLE = leftToRightGiving0.get(le);

			Set<V5> cForCE = new HashSet<>(Sets.intersection(bForBEAndCE, cForJE));

			Set<V5> dForDE = cForCE;

			Set<V5> gForAGAndAF = new HashSet<>(
					intersect3(leftToRightGiving0.get(ia), leftToRightGiving0.get(ja), leftToRightGiving0.get(la)));

			Set<V5> gForAG = new HashSet<>(Sets.intersection(gForAGAndAF, leftToRightGiving1.get(ka)));

			Set<V5> fForAF = new HashSet<>(Sets.intersection(gForAGAndAF, leftToRightGiving0.get(ka)));

			Set<V5> hForAH = fForAF;

			if (Arrays.asList(bForBE, cForCE, dForDE, fForAF, gForAG, hForAH)
					.stream()
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
					.build();

			Entry<String, Set<V5>> min = selectMin(nameToValues);

			LOGGER.trace("Min: {}", min);

			return toStream(ijkl, nameToValues, min);
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

	private static Stream<List<V5>> allIJKLAsStream() {
		return all()
				// Order IJKL matrix columns: we order the first columns
				// .filter(i -> i.isGrowing())
				.flatMap(i -> all().filter(j ->
		// j.isGrowing() &&
		j.isStrictlyAfter(i)).flatMap(j -> all().filter(k ->
		// k.isGrowing() &&
		k.isStrictlyAfter(j)).flatMap(k -> all().filter(l ->
		// l.isGrowing() &&
		l.isStrictlyAfter(k)).map(l -> Arrays.asList(i, j, k, l)))));
	}

	private static Stream<? extends V5> toStream(List<V5> list,
			Map<String, Set<V5>> nameToValues,
			Entry<String, Set<V5>> min) {
		return min.getValue().stream().flatMap(b -> {
			Map<String, Set<V5>> nameToValues2 = reduce(list, nameToValues, min, b);

			Entry<String, Set<V5>> min2 = selectMin(nameToValues2);

			if (min2.getValue().isEmpty()) {
				return Stream.empty();
			}

			return min2.getValue().stream().flatMap(c -> {
				Map<String, Set<V5>> nameToValues3 = reduce(list, nameToValues2, min2, c);

				Entry<String, Set<V5>> min3 = selectMin(nameToValues3);

				if (min3.getValue().isEmpty()) {
					return Stream.empty();
				}

				return min3.getValue().stream().flatMap(d -> {
					Map<String, Set<V5>> nameToValues4 = reduce(list, nameToValues3, min3, d);

					Entry<String, Set<V5>> min4 = selectMin(nameToValues4);

					if (min4.getValue().isEmpty()) {
						return Stream.empty();
					}

					return min4.getValue().stream().flatMap(f -> {
						Map<String, Set<V5>> nameToValues5 = reduce(list, nameToValues4, min4, f);

						Entry<String, Set<V5>> min5 = selectMin(nameToValues5);

						if (min5.getValue().isEmpty()) {
							return Stream.empty();
						}

						return min5.getValue().stream().flatMap(g -> {
							Map<String, Set<V5>> nameToValues6 = reduce(list, nameToValues5, min5, g);

							Entry<String, Set<V5>> min6 = selectMin(nameToValues6);

							if (min6.getValue().isEmpty()) {
								return Stream.empty();
							}

							return min5.getValue().stream().flatMap(h -> {
								Map<String, Set<V5>> nameToValues7 = reduce(list, nameToValues6, min6, h);

								Entry<String, Set<V5>> min7 = selectMin(nameToValues7);

								if (min7.getValue().isEmpty()) {
									return Stream.empty();
								}

								return min7.getValue().stream();
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

	private static Map<String, Set<V5>> reduce(List<V5> list,
			Map<String, Set<V5>> nameToValues,
			Entry<String, Set<V5>> min,
			V5 b) {
		return nameToValues.entrySet().stream().filter(ee -> !ee.getKey().equals(min.getKey())).collect(
				Collectors.toMap(ee -> ee.getKey(), ee -> restrict(list, min.getKey(), b, ee.getKey(), ee.getValue())));
	}

	private static Set<String> abcd = ImmutableSet.of("a", "b", "c", "d");
	private static Set<String> efgh = ImmutableSet.of("e", "f", "g", "h");

	private static final Set<Set<String>> bgdh = ImmutableSet.of(ImmutableSet.of("b", "g"), ImmutableSet.of("d", "h"));
	private static final Set<Set<String>> agch = ImmutableSet.of(ImmutableSet.of("a", "g"), ImmutableSet.of("c", "h"));
	private static final Set<Set<String>> bedf = ImmutableSet.of(ImmutableSet.of("b", "e"), ImmutableSet.of("d", "f"));
	private static final Set<Set<String>> aecf = ImmutableSet.of(ImmutableSet.of("a", "e"), ImmutableSet.of("c", "f"));

	private static Set<V5> restrict(List<V5> ijkl,
			String valueName,
			V5 valueEnforced,
			String candidateColumn,
			Set<V5> leftCandidates) {
		if (abcd.contains(valueName) && abcd.contains(candidateColumn)) {
			return leftCandidates;
		} else if (efgh.contains(valueName) && efgh.contains(candidateColumn)) {
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

	private static Stream<List<V5>> newStreamOfTriplets() {
		return newStreamOfNlets(3);
	}

	private static Stream<List<V5>> newStreamOfNlets(int n) {
		List<Set<V5>> asList = new ArrayList<>();

		IntStream.range(0, n).forEach(i -> asList.add(all().map(v5 -> v5).collect(Collectors.toSet())));

		return ImmutableList.copyOf(Sets.cartesianProduct(asList)).stream();
	}

	private static Stream<V5> all() {
		int power = 5;

		int limit = 1;
		for (int i = 0; i < power; i++) {
			// -1, 0, 1 -> 3 values
			limit *= 3;
		}

		int[] array = IntStream.range(0, power).map(i -> -1).toArray();

		return Stream.iterate(new V5(array), v5 -> v5.increment(-1, 1)).limit(limit);
	}
}
