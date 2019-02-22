package io.cormoran.strassen.v2;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class StrassenHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(StrassenHelper.class);

	final List<ABCD> allV4s;
	final List<A> allAs;
	final List<Greek> allGreeks;
	final List<M4> allM4s;
	final List<AE4> allAEs;

	final List<GreekAE4> nbV4AE4Gives0;
	final List<GreekAE4> nbV4AE4Gives1;

	public StrassenHelper() {
		allV4s = IntStream.range(0, ABCD.NB_V4_WING).mapToObj(index -> new ABCD(index)).collect(Collectors.toList());

		LOGGER.info("# V4: {}", allV4s.size());

		allAs = IntStream.range(0, A.NB_A_WING).mapToObj(index -> new A(index)).collect(Collectors.toList());

		LOGGER.info("# A: {}", allAs.size());

		allGreeks = IntStream.range(0, Greek.CARDINALITY_GREEK)
				.mapToObj(index -> new Greek(index))
				.collect(Collectors.toList());

		LOGGER.info("# Greek: {}", allGreeks.size());

		allM4s = IntStream.range(0, ABCD.NB_V4_WING * ABCD.NB_V4_WING)
				.mapToObj(index -> new M4(index % ABCD.NB_V4_WING, (index / ABCD.NB_V4_WING) % ABCD.NB_V4_WING))
				.collect(Collectors.toList());

		LOGGER.info("# M4: {}", allM4s.size());

		allAEs = IntStream.range(0, AE4.NB_AE_WING)
				.mapToObj(index -> new AE4(index % ABCD.NB_V4_WING))
				.collect(Collectors.toList());

		LOGGER.info("# AE4: {}", allAEs.size());

		nbV4AE4Gives0 = Lists.cartesianProduct(allGreeks, allAEs)
				.stream()
				.map(l -> new GreekAE4((Greek) l.get(0), (AE4) l.get(1)))
				.filter(l -> VectorOperations.scalarProduct(l.greek, l.ae4) == 0)
				.collect(Collectors.toList());
		LOGGER.info("# A4.AE4 == 0: {}, including # V4 == {}",
				nbV4AE4Gives0.size(),
				nbV4AE4Gives0.stream().map(v4ae4 -> v4ae4.greek).distinct().count());

		nbV4AE4Gives1 = Lists.cartesianProduct(allGreeks, allAEs)
				.stream()
				.map(l -> new GreekAE4((Greek) l.get(0), (AE4) l.get(1)))
				.filter(l -> VectorOperations.scalarProduct(l.greek, l.ae4) == 1)
				.collect(Collectors.toList());
		LOGGER.info("# A4.AE4 == 1: {}, including # V4 == {}",
				nbV4AE4Gives1.size(),
				nbV4AE4Gives1.stream().map(v4ae4 -> v4ae4.greek).distinct().count());
	}

	public Set<AE4> gives1(Greek alpha) {
		return gives1(nbV4AE4Gives1, alpha);
	}

	public Set<AE4> gives0(Greek alpha) {
		return gives0(nbV4AE4Gives0, alpha);
	}

	public Set<Greek> gives0(AE4 alpha) {
		return gives0(nbV4AE4Gives0, alpha);
	}

	private static Set<Greek> gives0(List<GreekAE4> nbV4AE4Gives0, AE4 ae) {
		return nbV4AE4Gives0.stream()
				.filter(v4ae4 -> v4ae4.ae4.equals(ae))
				.map(v4ae4 -> v4ae4.greek)
				.distinct()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<AE4> gives0(List<GreekAE4> nbV4AE4Gives0, Greek greek) {
		return nbV4AE4Gives0.stream()
				.filter(v4ae4 -> v4ae4.greek.equals(greek))
				.map(v4ae4 -> v4ae4.ae4)
				.distinct()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<AE4> gives1(List<GreekAE4> nbV4AE4Gives1, Greek greek) {
		return nbV4AE4Gives1.stream()
				.filter(e -> e.greek.equals(greek))
				.map(e -> e.ae4)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
