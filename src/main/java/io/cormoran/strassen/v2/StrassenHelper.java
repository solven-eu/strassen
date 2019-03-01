package io.cormoran.strassen.v2;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class StrassenHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(StrassenHelper.class);

	final Set<ABCD> allV4s;
	final Set<A> allAs;
	final Set<Greek> allGreeks;
	final Set<M4> allM4s;
	final Set<AE4> allAEs;

	final Set<GreekAE4> nbV4AE4Gives0;
	final Set<GreekAE4> nbV4AE4Gives1;

	final Cache<Greek, Set<AE4>> greekToGives1 = CacheBuilder.newBuilder().build();
	final Cache<Greek, Set<AE4>> greekToGives0 = CacheBuilder.newBuilder().build();
	final Cache<AE4, Set<Greek>> ae4ToGives0 = CacheBuilder.newBuilder().build();

	final Cache<List<Set<AE4>>, Set<AE4>> ae4Intersections = CacheBuilder.newBuilder().maximumSize(1024 * 16).build();

	public StrassenHelper() {
		allV4s = IntStream.range(0, ABCD.NB_V4_WING)
				.mapToObj(index -> new ABCD(index))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.info("# V4: {}", allV4s.size());

		allAs = IntStream.range(0, A.NB_A_WING)
				.mapToObj(index -> new A(index))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.info("# A: {}", allAs.size());

		allGreeks = IntStream.range(0, Greek.CARDINALITY_GREEK)
				.mapToObj(index -> new Greek(index))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.info("# Greek: {}", allGreeks.size());

		allM4s = IntStream.range(0, ABCD.NB_V4_WING * ABCD.NB_V4_WING)
				.mapToObj(index -> new M4(index % ABCD.NB_V4_WING, (index / ABCD.NB_V4_WING) % ABCD.NB_V4_WING))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.info("# M4: {}", allM4s.size());

		allAEs = IntStream.range(0, AE4.NB_AE_WING)
				.mapToObj(index -> new AE4(index))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.info("# AE4: {}", allAEs.size());

		nbV4AE4Gives0 = Sets.cartesianProduct(allGreeks, allAEs)
				.stream()
				.map(l -> new GreekAE4((Greek) l.get(0), (AE4) l.get(1)))
				.filter(l -> VectorOperations.scalarProduct(l.greek, l.ae4) == 0)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		LOGGER.info("# A4.AE4 == 0: {}, including # V4 == {}",
				nbV4AE4Gives0.size(),
				nbV4AE4Gives0.stream().map(v4ae4 -> v4ae4.greek).distinct().count());

		nbV4AE4Gives1 = Sets.cartesianProduct(allGreeks, allAEs)
				.stream()
				.map(l -> new GreekAE4((Greek) l.get(0), (AE4) l.get(1)))
				.filter(l -> VectorOperations.scalarProduct(l.greek, l.ae4) == 1)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		LOGGER.info("# A4.AE4 == 1: {}, including # V4 == {}",
				nbV4AE4Gives1.size(),
				nbV4AE4Gives1.stream().map(v4ae4 -> v4ae4.greek).distinct().count());
	}

	public Set<AE4> gives1(Greek alpha) {
		try {
			return greekToGives1.get(alpha, () -> gives1(nbV4AE4Gives1, alpha));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<AE4> gives0(Greek alpha) {
		try {
			return greekToGives0.get(alpha, () -> gives0(nbV4AE4Gives0, alpha));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<Greek> gives0(AE4 alpha) {
		try {
			return ae4ToGives0.get(alpha, () -> gives0(nbV4AE4Gives0, alpha));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private static Set<Greek> gives0(Collection<GreekAE4> nbV4AE4Gives0, AE4 ae) {
		return nbV4AE4Gives0.stream()
				.filter(v4ae4 -> v4ae4.ae4.equals(ae))
				.map(v4ae4 -> v4ae4.greek)
				.distinct()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<AE4> gives0(Collection<GreekAE4> nbV4AE4Gives0, Greek greek) {
		return nbV4AE4Gives0.stream()
				.filter(v4ae4 -> v4ae4.greek.equals(greek))
				.map(v4ae4 -> v4ae4.ae4)
				.distinct()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<AE4> gives1(Collection<GreekAE4> nbV4AE4Gives1, Greek greek) {
		return nbV4AE4Gives1.stream()
				.filter(e -> e.greek.equals(greek))
				.map(e -> e.ae4)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Set<AE4> intersection(Set<AE4> left, Set<AE4> right) {
		try {
			return ae4Intersections.get(Arrays.asList(left, right),
					() -> ImmutableSet.copyOf(Sets.intersection(left, right)));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
