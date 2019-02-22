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
	private static final boolean RELOAD_BINARY = false;

	public static void main(String[] args) {
		StrassenHelper helper = new StrassenHelper();

		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> alphaBetaGammaDeltaToCombinations;
		if (RELOAD_BINARY) {
			try {
				alphaBetaGammaDeltaToCombinations =
						PepperSerializationHelper.fromBytes(ByteStreams.toByteArray(new ClassPathResource(
								"/intermediate_binaries/alphaBetaGammaDeltaToCombinations_" + ABCD.NB_BLOCK
										+ "_"
										+ AE4.NB_MUL
										+ ".strassen").getInputStream()));
			} catch (ClassNotFoundException | IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			alphaBetaGammaDeltaToCombinations = computeCombinations(helper);

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

		AE4 zero = AE4.zero();

		Multimaps.asMap(alphaBetaGammaDeltaToCombinations).forEach((abgd, combinations) -> {
			Set<AE4> alphaTo0 = helper.gives0(abgd.getI(0))
					.stream()
					.filter(ae4 -> !ae4.equals(zero))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			Set<AE4> betaTo0 = helper.gives0(abgd.getI(1))
					.stream()
					.filter(ae4 -> !ae4.equals(zero))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			Set<AE4> gammaTo0 = helper.gives0(abgd.getI(2))
					.stream()
					.filter(ae4 -> !ae4.equals(zero))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			Set<AE4> deltaTo0 = helper.gives0(abgd.getI(3))
					.stream()
					.filter(ae4 -> !ae4.equals(zero))
					.collect(Collectors.toCollection(LinkedHashSet::new));

			// AG and AH gives 0 against Alpha, Beta, Gamma, Delta
			Set<AE4> abgdTo0 =
					Sets.intersection(Sets.intersection(alphaTo0, betaTo0), Sets.intersection(gammaTo0, deltaTo0));

			if (abgdTo0.isEmpty()) {
				return;
			}

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

					List<List<A>> aefghs = new ArrayList<>();
					helper.allAs.stream().forEach(a -> {

						LOGGER.info("a={}", a);
						helper.allAs.stream().filter(e -> ae.equals(VectorOperations.mul(a, e))).forEach(e -> {
							LOGGER.info("e={}", e);
							helper.allAs.stream().filter(f -> af.equals(VectorOperations.mul(a, f))).forEach(f -> {
								helper.allAs.stream().filter(g -> ag.equals(VectorOperations.mul(a, g))).forEach(g -> {
									helper.allAs.stream()
											.filter(h -> ah.equals(VectorOperations.mul(a, h)))
											.forEach(h -> {
												aefghs.add(Arrays.asList(a, e, f, g, h));
											});
								});
							});
						});

					});

					LOGGER.info("#aefgh: {}", aefghs.size());
				});
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

			File tmpFile =
					File.createTempFile("alphaBetaGammaDeltaToCombinations_" + ABCD.NB_BLOCK + "_" + AE4.NB_MUL + "_",
							".strassen");

			try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
				ByteStreams.copy(new ByteArrayInputStream(bytes), fos);
			}

			LOGGER.info("We have written {} bytes into {}", bytes.length, tmpFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> computeCombinations(StrassenHelper helper) {
		// values as ArrayList as we know we will not insert any duplicates
		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> alphaBetaGammaDeltaToCombinations =
				MultimapBuilder.linkedHashKeys().arrayListValues().build();

		AE4 ae4Zero = AE4.zero();

		List<Greek> alphas =
				helper.nbV4AE4Gives1.stream().map(v4ae4 -> v4ae4.greek).distinct().collect(Collectors.toList());
		alphas.forEach(alpha -> {
			// Filter Alpha.AE == 1
			Set<AE4> aes_Alpha = helper.gives1(alpha);
			// Filter Alpha.AF == 0
			Set<AE4> afs_Alpha = helper.gives0(alpha);
			// Filter Alpha.CE == 0
			Set<AE4> ces_Alpha = helper.gives0(alpha);
			// Filter Alpha.CF == 0
			Set<AE4> cfs_Alpha = helper.gives0(alpha);
			LOGGER.info("For alpha={} and # AE={} and # AF={}", alpha, aes_Alpha.size(), afs_Alpha.size());

			aes_Alpha.forEach(ae -> {
				// Filter (Beta|Gamma|Delta).AE == 0
				Set<Greek> betaGammaDeltas = helper.gives0(ae);

				LOGGER.debug("For alpha={} and AE={} # beta|gamma|delta={}", alpha, ae, betaGammaDeltas.size());

				betaGammaDeltas.forEach(beta -> {
					// Filter Beta.AF == 1
					Set<AE4> afs_Beta = helper.gives1(beta);
					Set<AE4> afs_AlphaBeta = Sets.intersection(afs_Alpha, afs_Beta);

					// Filter Alpha.CE == 0
					Set<AE4> ces_Beta = helper.gives0(beta);
					Set<AE4> ces_AlphaBeta = Sets.intersection(ces_Alpha, ces_Beta);
					Set<AE4> cfs_Beta = helper.gives0(beta);
					Set<AE4> cfs_AlphaBeta = Sets.intersection(cfs_Alpha, cfs_Beta);

					afs_AlphaBeta.forEach(af -> {
						betaGammaDeltas.forEach(gamma -> {
							if (gamma.equals(beta)) {
								return;
							} else if (VectorOperations.scalarProduct(gamma, af) != 0) {
								LOGGER.debug("AF not compatible with Gamma");
								return;
							}

							Set<AE4> ces_Gamma = helper.gives1(gamma);
							Set<AE4> ces_AlphaBetaGamma = Sets.intersection(ces_AlphaBeta, ces_Gamma);

							Set<AE4> cfs_Gamma = helper.gives0(gamma);
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

									Set<AE4> cfs_Delta = helper.gives1(delta);
									Set<AE4> cfs_AlphaBetaGammaDelta = Sets.intersection(cfs_AlphaBetaGamma, cfs_Delta);

									// Check if greek have at least one common AG giving 0
									{
										Set<AE4> toZero_Delta = helper.gives0(delta);
										Set<AE4> toZero_AlphaBetaGamma = cfs_AlphaBetaGamma;
										Set<AE4> toZero_AlphaBetaGammaDelta =
												Sets.intersection(toZero_AlphaBetaGamma, toZero_Delta);

										if (toZero_AlphaBetaGammaDelta.isEmpty()
												|| toZero_AlphaBetaGammaDelta.size() == 1
														&& toZero_AlphaBetaGammaDelta.contains(ae4Zero)) {
											return;
										}
									}

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

	@Deprecated
	private static void checkBug(StrassenHelper helper, AlphaBetaGammaDelta abgd) {
		AE4 zero = AE4.zero();
		Set<AE4> alphaTo0 = helper.gives0(abgd.getI(0))
				.stream()
				.filter(ae4 -> !ae4.equals(zero))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<AE4> betaTo0 = helper.gives0(abgd.getI(1))
				.stream()
				.filter(ae4 -> !ae4.equals(zero))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<AE4> gammaTo0 = helper.gives0(abgd.getI(2))
				.stream()
				.filter(ae4 -> !ae4.equals(zero))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<AE4> deltaTo0 = helper.gives0(abgd.getI(3))
				.stream()
				.filter(ae4 -> !ae4.equals(zero))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		// AG and AH gives 0 against Alpha, Beta, Gamma, Delta
		Set<AE4> abgdTo0 =
				Sets.intersection(Sets.intersection(alphaTo0, betaTo0), Sets.intersection(gammaTo0, deltaTo0));

		if (abgdTo0.isEmpty()) {
			throw new IllegalArgumentException("Empty but not empty X|");
		}
	}

	private static boolean checkAEZeroesIncompatibility3(AE4 af, AE4 ce, AE4 ae) {
		for (int i = 0; i < ABCD.NB_BLOCK; i++) {
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
}
