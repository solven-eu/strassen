package io.cormoran.strassen.v2;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * <p>
 * |A B| |E F| _ |AE+BG AF+BH| _ |alpha.M beta.M|
 * </p>
 * <p>
 * |C D| |G H| = |CE+DG CF+DH| = |gamma.M delta.M|
 * </p>
 * 
 * @author Benoit Lacelle
 *
 */
public class RunStrassen {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunStrassen.class);

	public static void main(String[] args) {
		List<V4> allV4s =
				IntStream.range(0, V4.NB_V4_WING).mapToObj(index -> new V4(index)).collect(Collectors.toList());

		LOGGER.info("# V4: {}", allV4s.size());

		List<M4> allM4s = IntStream.range(0, V4.NB_V4_WING * V4.NB_V4_WING)
				.mapToObj(index -> new M4(index % V4.NB_V4_WING, (index / V4.NB_V4_WING) % V4.NB_V4_WING))
				.collect(Collectors.toList());

		LOGGER.info("# M4: {}", allM4s.size());

		List<AE4> allAEs = IntStream.range(0, AE4.NB_AE_WING)
				.mapToObj(index -> new AE4(index % V4.NB_V4_WING))
				.collect(Collectors.toList());

		LOGGER.info("# AE4: {}", allAEs.size());

		List<V4AE4> nbV4AE4Gives0 = Lists.cartesianProduct(allV4s, allAEs)
				.stream()
				.map(l -> new V4AE4((V4) l.get(0), (AE4) l.get(1)))
				.filter(l -> VectorOperations.scalarProduct(l.v4, l.ae4) == 0)
				.collect(Collectors.toList());
		LOGGER.info("# A4.AE4 == 0: {}, including # V4 == {}",
				nbV4AE4Gives0.size(),
				nbV4AE4Gives0.stream().map(v4ae4 -> v4ae4.v4).distinct().count());

		List<V4AE4> nbV4AE4Gives1 = Lists.cartesianProduct(allV4s, allAEs)
				.stream()
				.map(l -> new V4AE4((V4) l.get(0), (AE4) l.get(1)))
				.filter(l -> VectorOperations.scalarProduct(l.v4, l.ae4) == 1)
				.collect(Collectors.toList());
		LOGGER.info("# A4.AE4 == 1: {}, including # V4 == {}",
				nbV4AE4Gives1.size(),
				nbV4AE4Gives1.stream().map(v4ae4 -> v4ae4.v4).distinct().count());

		List<V4> alphas = nbV4AE4Gives1.stream().map(v4ae4 -> v4ae4.v4).distinct().collect(Collectors.toList());
		alphas.forEach(alpha -> {
			// Filter Alpha.AE == 1
			Set<AE4> aes_Alpha = gives1(nbV4AE4Gives1, alpha);
			// Filter Alpha.AF == 0
			Set<AE4> afs_Alpha = gives0(nbV4AE4Gives0, alpha);
			// Filter Alpha.CE == 0
			Set<AE4> ces_Alpha = gives0(nbV4AE4Gives0, alpha);
			// Filter Alpha.CF == 0
			Set<AE4> cfs_Alpha = gives0(nbV4AE4Gives0, alpha);
			LOGGER.info("For alpha={} and # AE={} and # AF={}", alpha, aes_Alpha.size(), afs_Alpha.size());

			aes_Alpha.forEach(ae -> {
				// Filter (Beta|Gamma|Delta).AE == 0
				Set<V4> betaGammaDeltas = gives0(nbV4AE4Gives0, ae);

				LOGGER.info("For alpha={} and AE={} # beta|gamma|delta={}", alpha, ae, betaGammaDeltas.size());

				betaGammaDeltas.forEach(beta -> {
					// Filter Beta.AF == 1
					Set<AE4> afs_Beta = gives1(nbV4AE4Gives1, beta);
					Set<AE4> afs_AlphaBeta = Sets.intersection(afs_Alpha, afs_Beta);

					// Filter Alpha.CE == 0
					Set<AE4> ces_Beta = gives0(nbV4AE4Gives0, beta);
					Set<AE4> ces_AlphaBeta = Sets.intersection(ces_Alpha, ces_Beta);
					Set<AE4> cfs_Beta = gives0(nbV4AE4Gives0, beta);
					Set<AE4> cfs_AlphaBeta = Sets.intersection(cfs_Alpha, cfs_Beta);

					afs_AlphaBeta.forEach(af -> {
						betaGammaDeltas.forEach(gamma -> {
							if (gamma.equals(beta)) {
								return;
							} else if (gives0(nbV4AE4Gives0, gamma).contains(af)) {
								LOGGER.debug("AF not compatible with Gamma");
								return;
							}

							Set<AE4> ces_Gamma = gives1(nbV4AE4Gives1, gamma);
							Set<AE4> ces_AlphaBetaGamma = Sets.intersection(ces_AlphaBeta, ces_Gamma);
							Set<AE4> cfs_Gamma = gives1(nbV4AE4Gives1, gamma);
							Set<AE4> cfs_AlphaBetaGamma = Sets.intersection(cfs_AlphaBeta, cfs_Gamma);

							if (ces_AlphaBetaGamma.size() != 0) {
								LOGGER.info("For alpha={} beta={} gamma={} and ae={} and af={}, # CEs == {}",
										alpha,
										beta,
										gamma,
										ae,
										af,
										ces_AlphaBetaGamma.size());
							}
						});
					});
				});
			});
		});
	}

	private static Set<V4> gives0(List<V4AE4> nbV4AE4Gives0, AE4 ae) {
		return nbV4AE4Gives0.stream()
				.filter(v4ae4 -> v4ae4.ae4.equals(ae))
				.map(v4ae4 -> v4ae4.v4)
				.distinct()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<AE4> gives0(List<V4AE4> nbV4AE4Gives0, V4 ae) {
		return nbV4AE4Gives0.stream()
				.filter(v4ae4 -> v4ae4.v4.equals(ae))
				.map(v4ae4 -> v4ae4.ae4)
				.distinct()
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<AE4> gives1(List<V4AE4> nbV4AE4Gives1, V4 beta) {
		return nbV4AE4Gives1.stream()
				.filter(e -> e.v4.equals(beta))
				.map(e -> e.ae4)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
