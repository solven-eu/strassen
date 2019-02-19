package io.cormoran.strassen.v2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import cormoran.pepper.io.PepperSerializationHelper;

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
	private static final boolean RELOAD_BINARY = true;

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

		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> alphaBetaGammaDeltaToCombinations;
		if (RELOAD_BINARY) {
			try {
				alphaBetaGammaDeltaToCombinations = PepperSerializationHelper.fromBytes(ByteStreams.toByteArray(
						new ClassPathResource("/intermediate_binaries/alphaBetaGammaDeltaToCombinations.strassen")
								.getInputStream()));
			} catch (ClassNotFoundException | IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			alphaBetaGammaDeltaToCombinations = computeCombinations(nbV4AE4Gives0, nbV4AE4Gives1);

			serializeToTmpFile(alphaBetaGammaDeltaToCombinations);
		}

		Multimap<AE_AF_CE_CF, AlphaBetaGammaDelta> combinationsToAlphaBetaGammaDelta =
				MultimapBuilder.linkedHashKeys().arrayListValues().build();
		Multimaps.invertFrom(alphaBetaGammaDeltaToCombinations, combinationsToAlphaBetaGammaDelta);

		// 1433472 -> 246144 -> 246144
		LOGGER.info("# AlphaBetaGammaDelta={} # AE_AF_CE_CF={} # AlphaBetaGammaDelta_AE_AF_CE_CF={}",
				alphaBetaGammaDeltaToCombinations.keySet().size(),
				combinationsToAlphaBetaGammaDelta.keySet().size(),
				alphaBetaGammaDeltaToCombinations.size());

		// List<V4> as = allV4s;
		// List<V4> efghs = allV4s;

		// Multimap<List<V4>, AE_AF_CE_CF> AEFGH_To_AE_AF_AG_AH =
		// MultimapBuilder.linkedHashKeys().arrayListValues().build();
		// Lists.cartesianProduct(allV4s, allV4s, allV4s, allV4s, allV4s).forEach(l -> {
		// V4 a = l.get(0);
		// V4 e = l.get(1);
		// V4 f = l.get(2);
		// V4 g = l.get(3);
		// V4 h = l.get(4);
		//
		// AE4 ae = VectorOperations.mul(a, e);
		// AE4 af = VectorOperations.mul(a, f);
		// AE4 ag = VectorOperations.mul(a, g);
		// AE4 ah = VectorOperations.mul(a, h);
		// AEFGH_To_AE_AF_AG_AH.put(Arrays.asList(a, e, f, g, h), new AE_AF_CE_CF(ae, af, ag, ah));
		// });
		// Multimap<AE_AF_CE_CF, List<V4>> AE_AF_AG_AH_toAEFGH =
		// MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
		// Multimaps.invertFrom(AEFGH_To_AE_AF_AG_AH, AE_AF_AG_AH_toAEFGH);

		// AEFGH_To_AE_AF_AG_AH.keySet().size();
		// AE_AF_AG_AH_toAEFGH.keySet().size();

		Multimaps.asMap(alphaBetaGammaDeltaToCombinations).forEach((abgd, combinations) -> {
			Set<AE4> alphaTo0 = gives0(nbV4AE4Gives0, abgd.alpha);
			Set<AE4> betaTo0 = gives0(nbV4AE4Gives0, abgd.beta);
			Set<AE4> gammaTo0 = gives0(nbV4AE4Gives0, abgd.gamma);
			Set<AE4> deltaTo0 = gives0(nbV4AE4Gives0, abgd.delta);

			// AG and AH gives 0 against Alpha, Beta, Gamma, Delta
			Set<AE4> abgdTo0 =
					Sets.intersection(Sets.intersection(alphaTo0, betaTo0), Sets.intersection(gammaTo0, deltaTo0));

			Lists.cartesianProduct(combinations, combinations).stream().forEach(combination -> {
				AE_AF_CE_CF ae_af_ce_cf = combination.get(0);
				AE4 ae = ae_af_ce_cf.ae;
				AE4 af = ae_af_ce_cf.af;
				AE_AF_CE_CF bg_bh_dg_dh = combination.get(1);

				Set<AE4> ags = abgdTo0.stream()
						// AE and BG constrains AG
						.filter(ag -> checkAEZeroesIncompatibility3(ae_af_ce_cf.ae, bg_bh_dg_dh.ae, ag))
						// AE and DG constrains AG
						.filter(ag -> checkAEZeroesIncompatibility3(ae_af_ce_cf.ae, bg_bh_dg_dh.ce, ag))
						// AF and BG constrains AG
						.filter(ag -> checkAEZeroesIncompatibility3(ae_af_ce_cf.af, bg_bh_dg_dh.ae, ag))
						// AF and DG constrains AG
						.filter(ag -> checkAEZeroesIncompatibility3(ae_af_ce_cf.af, bg_bh_dg_dh.ce, ag))
						.collect(Collectors.toCollection(LinkedHashSet::new));

				Set<AE4> ahs = abgdTo0.stream()
						// AE and BH constrains AH
						.filter(ah -> checkAEZeroesIncompatibility3(ae_af_ce_cf.ae, bg_bh_dg_dh.af, ah))
						// AE and DH constrains AH
						.filter(ah -> checkAEZeroesIncompatibility3(ae_af_ce_cf.ae, bg_bh_dg_dh.cf, ah))
						// AF and BH constrains AH
						.filter(ah -> checkAEZeroesIncompatibility3(ae_af_ce_cf.af, bg_bh_dg_dh.af, ah))
						// AF and DH constrains AH
						.filter(ah -> checkAEZeroesIncompatibility3(ae_af_ce_cf.af, bg_bh_dg_dh.cf, ah))
						.collect(Collectors.toCollection(LinkedHashSet::new));

				Sets.cartesianProduct(ags, ahs).stream().forEach(ag_ah -> {
					AE4 ag = ag_ah.get(0);
					AE4 ah = ag_ah.get(1);

					List<List<V4>> aefghs = new ArrayList<>();
					allV4s.stream().forEach(a -> {

						LOGGER.info("a={}", a);
						allV4s.stream().filter(e -> ae.equals(VectorOperations.mul(a, e))).forEach(e -> {

							LOGGER.info("e={}", e);
							allV4s.stream().filter(f -> af.equals(VectorOperations.mul(a, f))).forEach(f -> {
								allV4s.stream().filter(g -> ag.equals(VectorOperations.mul(a, g))).forEach(g -> {
									allV4s.stream().filter(h -> ah.equals(VectorOperations.mul(a, h))).forEach(h -> {
										aefghs.add(Arrays.asList(a, e, f, g, h));
									});
								});
							});
						});

					});

					LOGGER.info("#aefgh: {}", aefghs.size());

					// AE_AF_CE_CF ae_af_ag_ah = new AE_AF_CE_CF(ae, af, ag, ah);
					// AE_AF_AG_AH_toAEFGH.get(ae_af_ag_ah).forEach(aefgh -> {
					// V4 a = aefgh.get(0);
					// V4 e = aefgh.get(1);
					// V4 f = aefgh.get(2);
					// V4 g = aefgh.get(3);
					// V4 h = aefgh.get(4);
					//
					// LOGGER.info("a={} e={} f={} g={} h={} ", a, e, f, g, h);
					// });
					// //
					// // allM4s.stream().forEach(abcd_efgh_0 -> {
					// // // for (int i = 0; i < V4.NB_BLOCK; i++) {
					// // // Check AE
					// // {
					// // int a_0 = abcd_efgh_0.left.getI(0);
					// //
					// // for (int j = 0; j < V4.NB_BLOCK; j++) {
					// // if (a_0 * abcd_efgh_0.right.getI(j) != ae.getI(j)) {
					// // return;
					// // }
					// // }
					// //
					// // for (int j = 0; j < V4.NB_BLOCK; j++) {
					// // if (a_0 * abcd_efgh_0.right.getI(j) != ae.getI(j)) {
					// // return;
					// // }
					// // }
					// // }
					// // // }
					// // });
					//
					// // Lists.cartesianProduct(allV4s, allV4s).forEach(a_efgh -> {
					// // V4 a = a_efgh.get(0);
					// // V4 efgh = a_efgh.get(1);
					// //
					// // VectorOperations.scalarProduct(v4, ae4);
					// // });
				});
				// }
				if (!ags.isEmpty() && !ahs.isEmpty()) {
					LOGGER.info("# AG={} # AH={}", ags.size(), ahs.size());
				}
			});
		});

	}

	private static void serializeToTmpFile(
			ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> alphaBetaGammaDeltaToCombinations) {
		try {
			byte[] bytes = PepperSerializationHelper.toBytes((Serializable) alphaBetaGammaDeltaToCombinations);

			File tmpFile = File.createTempFile("alphaBetaGammaDeltaToCombinations", ".strassen");

			try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
				ByteStreams.copy(new ByteArrayInputStream(bytes), fos);
			}

			LOGGER.info("We have written {} bytes into {}", bytes.length, tmpFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> computeCombinations(List<V4AE4> nbV4AE4Gives0,
			List<V4AE4> nbV4AE4Gives1) {
		// values as ArrayList as we know we will not insert any duplicates
		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> alphaBetaGammaDeltaToCombinations =
				MultimapBuilder.linkedHashKeys().arrayListValues().build();

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

				LOGGER.debug("For alpha={} and AE={} # beta|gamma|delta={}", alpha, ae, betaGammaDeltas.size());

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
							} else if (VectorOperations.scalarProduct(gamma, af) != 0) {
								LOGGER.debug("AF not compatible with Gamma");
								return;
							}

							Set<AE4> ces_Gamma = gives1(nbV4AE4Gives1, gamma);
							Set<AE4> ces_AlphaBetaGamma = Sets.intersection(ces_AlphaBeta, ces_Gamma);

							Set<AE4> cfs_Gamma = gives0(nbV4AE4Gives0, gamma);
							Set<AE4> cfs_AlphaBetaGamma = Sets.intersection(cfs_AlphaBeta, cfs_Gamma);

							ces_AlphaBetaGamma.forEach(ce -> {
								if (!checkAEZeroesIncompatibility3(af, ce, ae)) {
									return;
								}

								betaGammaDeltas.forEach(delta -> {
									if (delta.equals(beta) || delta.equals(gamma)) {
										return;
									} else if (VectorOperations.scalarProduct(delta, af) != 0) {
										LOGGER.debug("AF not compatible with Delta");
										return;
									} else if (VectorOperations.scalarProduct(delta, ce) != 0) {
										LOGGER.debug("CE not compatible with Delta");
										return;
									}

									AlphaBetaGammaDelta abgd = new AlphaBetaGammaDelta(alpha, beta, gamma, delta);

									Set<AE4> cfs_Delta = gives1(nbV4AE4Gives1, delta);
									Set<AE4> cfs_AlphaBetaGammaDelta = Sets.intersection(cfs_AlphaBetaGamma, cfs_Delta);

									cfs_AlphaBetaGammaDelta.forEach(cf -> {
										if (!checkAEZeroesIncompatibility3(af, ce, cf)) {
											return;
										} else if (!checkAEZeroesIncompatibility3(ae, cf, ce)) {
											return;
										} else if (!checkAEZeroesIncompatibility3(ae, cf, af)) {
											return;
										}

										alphaBetaGammaDeltaToCombinations.put(abgd, new AE_AF_CE_CF(ae, af, ce, cf));

										if (Long.highestOneBit(alphaBetaGammaDeltaToCombinations.size()) != Long
												.highestOneBit(alphaBetaGammaDeltaToCombinations.size() - 1)) {

											LOGGER.info(
													"({}) For Alpha={} Beta={} Gamma={} Delta={} and AE={} and AF={} and CE={}, CF={}",
													alphaBetaGammaDeltaToCombinations.size(),
													alpha,
													beta,
													gamma,
													delta,
													ae,
													af,
													ce,
													cf);
										}
									});

								});
							});
						});
					});
				});
			});
		});
		return alphaBetaGammaDeltaToCombinations;
	}

	private static boolean checkAEZeroesIncompatibility3(AE4 af, AE4 ce, AE4 ae) {
		for (int i = 0; i < V4.NB_BLOCK; i++) {
			int aeValue = ae.getI(i);
			int afValue = af.getI(i);
			int ceValue = ce.getI(i);

			// If both AF and CE are not 0 at given index, then none of (A,F,C,E) is zero, then AE is not zero
			if (afValue != 0 && ceValue != 0 && aeValue == 0) {
				LOGGER.debug("At index={} AF and CE are not 0 while AE is 0", i);
				return false;
			}
		}

		return true;
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
