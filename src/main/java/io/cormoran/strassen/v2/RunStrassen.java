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
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import cormoran.pepper.io.PepperSerializationHelper;

/**
 * Given 2 matrices P and Q, each splitted in 4 sub-matrices (P -> A B C D, Q -> E F G H), we look for a way of
 * combining the 8 sub-matrices to compute the original 2 matrices product. The combination shall rely on N
 * multiplications of linear combinations of (A B C D) and (E F G H) : (?Ma+?Mb+?Mc+?Md)*(?Me+?Mf+?Mg+?Mh) and (N-1)
 * other combinations. Alpha, Beta, Gamma and Delta are the linear combinations of M rows giving back the quadrants of
 * the original matrix multiplication.
 * <p>
 * _ _ _ _ |A B| |E F| > |AE+BG AF+BH| > |alpha.M beta.M|
 * </p>
 * <p>
 * |P.Q| = |C D| |G H| = |CE+DG CF+DH| = |gamma.M delta.M|
 * </p>
 * 
 * Symmetries:
 * 
 * Symmetry 1: Greek and M coefficients can be re-ordered accordingly: we can consider only growing permutations of one
 * of greek (typically Alpha as it is the first considered greek).
 * 
 * Symmetry 2: If AC and BD columns are exchanged, is it directly compensated by exchanging rows EF and GH without
 * effect on greeks/Ms. (Swap A<->B, C<->D, E<->G, F<->H)
 * 
 * Symmetry 3: If AB and CD rows are exchanged, it is directly compensated by exchanging columns EG and FH and leads to
 * swapping of Alpha <-> Delta and Beta <-> Gamma. (Swap A<->C, B<->D, E<->F, G<->H)
 * 
 * Symmetry 4: Given Transposed(P.Q) == Transposed(Q).Transposed(P), we can transpose ABCD and EFGH to swap Gamma and
 * Beta. (https://en.wikipedia.org/wiki/Transpose)
 * 
 * ---
 * 
 * Symmetry 4 enables considering only Beta < Gamma
 * 
 * Symmetry 3,4 enables considering only Alpha < Delta
 * 
 * Symmetry 1 should implies considering the highest permutation of each Alpha, to restrict the possibilities for Delta
 * 
 * @author Benoit Lacelle
 *
 */
public class RunStrassen {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunStrassen.class);
	private static final boolean RELOAD_BINARY = false;

	// Safe is 0. We might want to restrict the considered Alpha.
	static final int DANGER_SKIP_ALPHA = 17;

	public static void main(String[] args) {
		StrassenHelper helper = new StrassenHelper();

		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> greeksToCombinations =
				computeOrReloadGreeksToCombinations(helper);

		Multimap<AE_AF_CE_CF, AlphaBetaGammaDelta> combinationsToAlphaBetaGammaDelta =
				MultimapBuilder.linkedHashKeys().arrayListValues().build();
		Multimaps.invertFrom(greeksToCombinations, combinationsToAlphaBetaGammaDelta);

		LOGGER.info("#Greeks={} #AE_AF_CE_CF={} #AlphaBetaGammaDelta_AE_AF_CE_CF={}",
				greeksToCombinations.keySet().size(),
				combinationsToAlphaBetaGammaDelta.keySet().size(),
				greeksToCombinations.size());

		AE4 zero = AE4.zero();

		Multimaps.asMap(greeksToCombinations).forEach((abgd, combinations) -> {
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

				// System.out.println(abgdTo0.stream()
				// // AE and BG constrains AG
				// .filter(ag -> checkAEZeroesIncompatibility3(ae_af_ce_cf.ae, bg_bh_dg_dh.ae, ag))
				// .count());

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

	private static ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> computeOrReloadGreeksToCombinations(
			StrassenHelper helper) {
		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> greeksToCombinations;
		if (RELOAD_BINARY) {
			try {
				greeksToCombinations =
						PepperSerializationHelper.fromBytes(ByteStreams.toByteArray(new ClassPathResource(
								"/intermediate_binaries/alphaBetaGammaDeltaToCombinations_" + ABCD.NB_BLOCK
										+ "_"
										+ AE4.NB_MUL
										+ ".strassen").getInputStream()));
			} catch (ClassNotFoundException | IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			greeksToCombinations = ComputeGreeksAndM.computeCombinations(helper);

			serializeToTmpFile(greeksToCombinations);
		}
		return greeksToCombinations;
	}

	private static boolean checkAEZeroesIncompatibility3(AE4 af, AE4 ce, AE4 ae) {
		return ComputeGreeksAndM.checkAEZeroesIncompatibility3(af, ce, ae);
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
}
