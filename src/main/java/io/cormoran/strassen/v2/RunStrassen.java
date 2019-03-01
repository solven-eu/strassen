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
 * |A B| |E F| > |AE+BG AF+BH| > |alpha.M beta.M|
 * </p>
 * <p>
 * |C D| |G H| = |CE+DG CF+DH| = |gamma.M delta.M|
 * </p>
 * 
 * Symmetries:
 * 
 * Symmetry 1: Greek and M coefficients can be re-ordered accordingly: we can consider only growing permutations of one
 * of greek (typically Alpha as it is the first considered greek).
 * 
 * Symmetry 2: If AC and BD columns are exchanged, is it directly compensated by exchanging rows EF and GH without
 * effect on greeks/Ms.
 * 
 * Symmetry 3: If AB and CD rows are exchanged, it is directly compensated by exchanging columns EG and FH and leads to
 * swapping of Alpha <-> Delta and Beta <-> Gamma.
 * 
 * Symmetry 4: Given Transposed(P.Q) == Transposed(Q).Transposed(Q), we can transpose ABCD and EFGH to swap Gamma and
 * Beta.
 * 
 * Symmetry 4 enables considering only Beta < Gamma
 * 
 * Symmetry 2,3,4 enables considering only Alpha < Delta
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
			Set<AE4> abgdTo0 = helper.intersection(helper.intersection(alphaTo0, betaTo0),
					helper.intersection(gammaTo0, deltaTo0));

			if (abgdTo0.isEmpty()) {
				return;
			}

			Lists.cartesianProduct(combinations, combinations).stream().forEach(combination -> {
				AE_AF_CE_CF ae_af_ce_cf = combination.get(0);
				AE4 ae = ae_af_ce_cf.ae;
				AE4 af = ae_af_ce_cf.af;
				AE_AF_CE_CF bg_bh_dg_dh = combination.get(1);

				System.out.println(abgdTo0.stream()
						// AE and BG constrains AG
						.filter(ag -> checkAEZeroesIncompatibility3(ae_af_ce_cf.ae, bg_bh_dg_dh.ae, ag))
						.count());

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

		List<Greek> alphas = helper.nbV4AE4Gives1.stream()
				.map(v4ae4 -> v4ae4.greek)
				.distinct()
				// We filter Alpha with index in growing order, as there is a symmetry between Greeks and Mi (swapping
				// entry in Alpha is a no-op if other greeks and Mi/AE4 are swapped the same way)
				.filter(greek -> Greek.isSoftlyGrowing(greek))
				.collect(Collectors.toList());

		LOGGER.info("# alpha={}", alphas.size());

		alphas.stream().limit(Integer.MAX_VALUE).forEach(alpha -> {
			// Filter Alpha.AE == 1
			Set<AE4> aes_Alpha = helper.gives1(alpha);

			Set<AE4> alphaTo0 = helper.gives0(alpha);
			LOGGER.info("For alpha={} and # AE={} and # alphaTo0={}", alpha, aes_Alpha.size(), alphaTo0.size());

			aes_Alpha.forEach(ae -> {
				// Filter (Beta|Gamma|Delta).AE == 0
				Set<Greek> betaGammaDeltas = helper.gives0(ae);

				LOGGER.debug("For alpha={} and AE={} # beta|gamma|delta={}", alpha, ae, betaGammaDeltas.size());

				betaGammaDeltas.stream().filter(beta -> {

					return true;
				}).limit(Integer.MAX_VALUE).forEach(beta -> {
					// Filter Beta.AF == 1
					Set<AE4> afs_Beta = helper.gives1(beta);
					Set<AE4> afs_AlphaBeta = helper.intersection(alphaTo0, afs_Beta);

					Set<AE4> alphaBetaTo0 = helper.intersection(alphaTo0, helper.gives0(beta));

					afs_AlphaBeta.forEach(af -> {
						betaGammaDeltas.forEach(gamma -> {
							if (gamma.equals(beta)) {
								return;
							} else if (VectorOperations.scalarProduct(gamma, af) != 0) {
								LOGGER.debug("AF not compatible with Gamma");
								return;
							}

							Set<AE4> ces_Gamma = helper.gives1(gamma);
							Set<AE4> ces_AlphaBetaGamma = helper.intersection(alphaBetaTo0, ces_Gamma);

							Set<AE4> alphaBetaGammaTo0 = helper.intersection(alphaBetaTo0, helper.gives0(gamma));

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
									Set<AE4> cfs_AlphaBetaGammaDelta =
											helper.intersection(alphaBetaGammaTo0, cfs_Delta);

									// Check if greek have at least one common AG giving 0
									{
										Set<AE4> toZero_AlphaBetaGammaDelta =
												helper.intersection(alphaBetaGammaTo0, helper.gives0(delta));

										int greeksToZeroSize = toZero_AlphaBetaGammaDelta.size();
										if (greeksToZeroSize == 0 || greeksToZeroSize == 1
												&& toZero_AlphaBetaGammaDelta.contains(ae4Zero)) {
											return;
										}
									}

									cfs_AlphaBetaGammaDelta.forEach(cf -> {
										// Check CF is compatible with AF and CE
										if (!checkAEZeroesIncompatibility3(af, ce, cf)) {
											return;
										} else if (!checkAEZeroesIncompatibility3(ae, cf, ce)) {
											return;
										} else if (!checkAEZeroesIncompatibility3(ae, cf, af)) {
											return;
										}

										alphaBetaGammaDeltaToCombinations.put(abgd, new AE_AF_CE_CF(ae, af, ce, cf));

										if (Long.bitCount(alphaBetaGammaDeltaToCombinations.size()) == 1) {
											LOGGER.info(
													"For Alpha={} Beta={} Gamma={} Delta={} and AE={} and AF={} and CE={}, CF={} ({})",
													alpha,
													beta,
													gamma,
													delta,
													ae,
													af,
													ce,
													cf,
													alphaBetaGammaDeltaToCombinations.size());
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
				helper.intersection(helper.intersection(alphaTo0, betaTo0), helper.intersection(gammaTo0, deltaTo0));

		if (abgdTo0.isEmpty()) {
			throw new IllegalArgumentException("Empty but not empty X|");
		}
	}

	private static boolean checkAEZeroesIncompatibility3(AE4 af, AE4 ce, AE4 ae) {
		for (int i = 0; i < AE4.MAX_AE; i++) {
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
