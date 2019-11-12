package io.cormoran.strassen.v2;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class ComputeGreeksAndM {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeGreeksAndM.class);

	private static final Multimap<AlphaBetaGammaDelta, AE_AF_CE_CF> EMPTY = ImmutableMultimap.of();

	private static final AtomicLong COUNT = new AtomicLong();

	/**
	 * Computes all possibles combinations for each AlphaBetaGammaDelta
	 * 
	 * @param helper
	 * @return
	 */
	public static ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> computeCombinations(StrassenHelper helper) {
		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> greeksToCombinations = newGreeksToCombinations();

		SetMultimap<Greek, Greek> alphasToDeltas = computeAlphasToDeltas(helper);

		AE4 ae4Zero = AE4.zero();

		Set<Greek> alphas = alphasToDeltas.keySet();
		alphas.stream().limit(Integer.MAX_VALUE).skip(RunStrassen.DANGER_SKIP_ALPHA).forEach(alpha -> {
			// Filter Alpha.AE == 1
			Set<AE4> aes_Alpha = helper.gives1(alpha);

			Set<AE4> alphaTo0 = helper.gives0(alpha);
			LOGGER.info("For alpha={} #AE={} and #alphaTo0={}", alpha, aes_Alpha.size(), alphaTo0.size());

			Set<Greek> deltas = alphasToDeltas.get(alpha);

			if (aes_Alpha.size() < deltas.size()) {
				aes_Alpha.forEach(ae -> {
					// Filter (Beta|Gamma|Delta).AE == 0
					Set<Greek> ae_to0 =
							helper.gives0(ae).stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));

					LOGGER.debug("For alpha={} and AE={} #beta|gamma|delta={}", alpha, ae, ae_to0.size());

					Sets.intersection(ae_to0, deltas).stream().sorted().limit(Integer.MAX_VALUE).forEach(delta -> {
						Set<AE4> deltaTo0 = helper.gives0(delta);

						// TODO This seems useless
						// if (!deltaTo0.contains(ae)) {
						// throw new IllegalArgumentException();
						// }

						greeksToCombinations.putAll(
								processGivenAlphaDelta(helper, ae4Zero, alpha, alphaTo0, ae, ae_to0, delta, deltaTo0));
					});
				});
			} else {
				deltas.stream().sorted().limit(Integer.MAX_VALUE).forEach(delta -> {
					Set<AE4> deltaTo0 = helper.gives0(delta);

					Set<AE4> aesAlphaDelta = Sets.intersection(deltaTo0, aes_Alpha);

					aesAlphaDelta.forEach(ae -> {
						// Filter (Beta|Gamma|Delta).AE == 0
						Set<Greek> ae_to0 = helper.gives0(ae)
								.stream()
								.sorted()
								.collect(Collectors.toCollection(LinkedHashSet::new));

						LOGGER.debug("For alpha={} and AE={} #beta|gamma|delta={}", alpha, ae, ae_to0.size());

						greeksToCombinations.putAll(
								processGivenAlphaDelta(helper, ae4Zero, alpha, alphaTo0, ae, ae_to0, delta, deltaTo0));
					});
				});
			}
		});
		return greeksToCombinations;
	}

	private static ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> newGreeksToCombinations() {
		// values as ArrayList as we know we will not insert any duplicates
		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> greeksToCombinations =
				MultimapBuilder.linkedHashKeys().arrayListValues().build();
		return greeksToCombinations;
	}

	private static SetMultimap<Greek, Greek> computeAlphasToDeltas(StrassenHelper helper) {
		SetMultimap<Greek, Greek> alphasToDeltas;
		{
			// We could restrict Alpha to vector being able to produce 1 and 0 (as Alpha on M(AE) will have to given 1
			// while it will have to give 0 on M(AF))
			Set<Greek> greekTo0 = helper.greekWithAE4To0.stream()
					.map(v4ae4 -> v4ae4.greek)
					.distinct()
					.collect(Collectors.toCollection(LinkedHashSet::new));
			Set<Greek> greekTo1 = helper.greekWithAE4To1.stream()
					.map(v4ae4 -> v4ae4.greek)
					.distinct()
					.collect(Collectors.toCollection(LinkedHashSet::new));

			Set<Greek> rawAlphasOrDelta = Sets.intersection(greekTo0, greekTo1);

			alphasToDeltas = computeAlphasToDeltas(rawAlphasOrDelta);

			LOGGER.info("#alphasToDeltas={} alphas={}", alphasToDeltas.size(), alphasToDeltas.keySet().size());

			alphasToDeltas.asMap()
					.forEach((alpha, deltas) -> LOGGER.info("Alpha={} -> #Delta={}", alpha, deltas.size()));
		}
		return alphasToDeltas;
	}

	/**
	 * This takes advantage of various symmetries, including Symmetry-1 a single permutation of Alpha can be considered
	 * (as (Greeks, Ms) can be permuted all accordingly). This symmetry is considered after restricting Alpha < Delta
	 * (given Symmetry-2,3,4)
	 * 
	 * @param rawAlphasOrDelta
	 * @return the possible pairs (Alpha, Deltas)
	 */
	private static SetMultimap<Greek, Greek> computeAlphasToDeltas(Set<Greek> rawAlphasOrDelta) {
		SetMultimap<Greek, Greek> alphasToDeltas = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();

		Sets.cartesianProduct(rawAlphasOrDelta, rawAlphasOrDelta)
				.stream()
				// Delta > Alpha
				// This typically rejects Alpha == [1,1,1,1,1] as then, there is no possible delta
				.filter(l -> l.get(1).compareTo(l.get(0)) > 0)
				.forEach(alphaBeta -> {
					// Given a possible Alpha and Delta, permute them to minimize A then D
					Greek alpha = alphaBeta.get(0);
					Greek delta = alphaBeta.get(1);

					// TODO Permute Alpha to get it softly growing
					int[] newA = new int[Greek.NB_COEF];
					int[] newD = new int[Greek.NB_COEF];

					Set<Integer> processedIndex = new LinkedHashSet<>();

					for (int i = 0; i < Greek.NB_COEF; i++) {
						// The minimum value taken by A: minA
						int minA = IntStream.range(0, Greek.NB_COEF)
								.filter(index -> !processedIndex.contains(index))
								.map(index -> alpha.getI(index))
								.min()
								.getAsInt();

						// The minimum value taken by D where A == minA: minD
						int minD = IntStream.range(0, Greek.NB_COEF)
								.filter(index -> !processedIndex.contains(index))
								.filter(index -> minA == alpha.getI(index))
								.map(index -> delta.getI(index))
								.min()
								.getAsInt();

						// Any index where A == min and D == minD
						int nextIndex = IntStream.range(0, Greek.NB_COEF)
								.filter(index -> !processedIndex.contains(index))
								.filter(index -> minA == alpha.getI(index))
								.filter(index -> minD == delta.getI(index))
								.findAny()
								.getAsInt();

						// This index should not be considered anymore as it is already inserted in output
						processedIndex.add(nextIndex);

						// write A and D by minimizing A then D
						newA[i] = alpha.getI(nextIndex);
						newD[i] = delta.getI(nextIndex);
					}

					Greek newAlpha = new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, newA));
					Greek newDelta = new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, newD));
					alphasToDeltas.put(newAlpha, newDelta);
				});
		return alphasToDeltas;
	}

	private static ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> processGivenAlphaDelta(StrassenHelper helper,
			AE4 ae4Zero,
			Greek alpha,
			Set<AE4> alphaTo0,
			AE4 ae,
			Set<Greek> ae_to0,
			Greek delta,
			Set<AE4> deltaTo0) {
		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> greeksToCombinations = newGreeksToCombinations();
		// Filter Delta.CF == 1
		Set<AE4> cfs_Delta = helper.gives1(delta);

		// Filter Alpha.CF == 0
		Set<AE4> cfs_AlphaDelta = helper.intersection(alphaTo0, cfs_Delta);

		Set<AE4> alphaDeltaTo0 = helper.intersection(alphaTo0, deltaTo0);

		cfs_AlphaDelta.forEach(cf -> {
			// TODO Restrict Gamma to minimal permutations keeping stable Alpha and Delta
			ae_to0.forEach(gamma -> {
				if (gamma.equals(delta)) {
					return;
				} else if (VectorOperations.scalarProduct(gamma, cf) != 0) {
					LOGGER.debug("CF not compatible with Gamma");
					return;
				}

				if (!isMinimized(gamma, alpha, delta)) {
					return;
				}

				// Filter Gamma.CE == 1
				Set<AE4> ces_Gamma = helper.gives1(gamma);
				// Filter Alpha.CE == 0
				// Filter Delta.CE == 0
				Set<AE4> ces_AlphaDeltaGamma = helper.intersection(alphaDeltaTo0, ces_Gamma);

				Set<AE4> alphaDeltaGammaTo0 = helper.intersection(alphaDeltaTo0, helper.gives0(gamma));

				ces_AlphaDeltaGamma.forEach(ce -> {
					// AE, CF, CE
					if (!checkAEZeroesIncompatibility3(ae, cf, ce)) {
						return;
					}

					// TODO Restrict Beta to minimal permutations keeping stable Alpha, Delta and Gamma
					ae_to0.stream()
							.filter(beta -> {
								// Symmetry: Gamma > Beta
								return beta.compareTo(gamma) > 0;
							})
							.forEach(beta -> {

								greeksToCombinations.putAll(processFurther(helper,
										ae4Zero,
										alpha,
										ae,
										delta,
										cf,
										gamma,
										alphaDeltaGammaTo0,
										ce,
										beta));
							});
				});
			});
		});

		return greeksToCombinations;
	}

	private static boolean isMinimized(Greek checkMe, Greek... early) {
		// TODO Auto-generated method stub
		return false;
	}

	private static Multimap<AlphaBetaGammaDelta, AE_AF_CE_CF> processFurther(StrassenHelper helper,
			AE4 ae4Zero,
			Greek alpha,
			AE4 ae,
			Greek delta,
			AE4 cf,
			Greek gamma,
			Set<AE4> alphaDeltaGammaTo0,
			AE4 ce,
			Greek beta) {
		if (beta.equals(delta) || beta.equals(gamma)) {
			return EMPTY;
		} else if (VectorOperations.scalarProduct(beta, cf) != 0) {
			LOGGER.debug("AF not compatible with Beta");
			return EMPTY;
		} else if (VectorOperations.scalarProduct(beta, ce) != 0) {
			LOGGER.debug("CE not compatible with Beta");
			return EMPTY;
		}

		AlphaBetaGammaDelta greeks = new AlphaBetaGammaDelta(alpha, beta, gamma, delta);
		{
			// TODO Check we check the matrix has some rank?
			// Check we do not consider dummy combinations
			nextCoef: for (int i = 0; i < Greek.NB_COEF; i++) {
				for (int j = 0; j < AlphaBetaGammaDelta.NB_GREEK; j++) {
					if (greeks.greeks[j].getI(i) != 0) {
						continue nextCoef;
					}
				}

				return EMPTY;
			}
		}

		Set<AE4> afs_Beta = helper.gives1(beta);
		Set<AE4> afs_AlphaBetaGammaDelta = helper.intersection(alphaDeltaGammaTo0, afs_Beta);

		// Check if greek have at least one common AG giving 0
		{
			Set<AE4> toZero_AlphaBetaGammaDelta = helper.intersection(alphaDeltaGammaTo0, helper.gives0(beta));

			int greeksToZeroSize = toZero_AlphaBetaGammaDelta.size();
			if (greeksToZeroSize == 0 || greeksToZeroSize == 1 && toZero_AlphaBetaGammaDelta.contains(ae4Zero)) {
				return EMPTY;
			}
		}

		ListMultimap<AlphaBetaGammaDelta, AE_AF_CE_CF> greeksToCombinations = newGreeksToCombinations();

		afs_AlphaBetaGammaDelta.forEach(af -> {
			// Check CF is compatible with AF and CE
			if (!checkAEZeroesIncompatibility3(af, ce, cf)) {
				return;
			} else if (!checkAEZeroesIncompatibility3(af, ce, ae)) {
				return;
			} else if (!checkAEZeroesIncompatibility3(ae, cf, af)) {
				return;
			}

			AE_AF_CE_CF aeAfCeCf = new AE_AF_CE_CF(ae, af, ce, cf);

			// if (!reject) {
			greeksToCombinations.put(greeks, aeAfCeCf);

			// Print a few valid combinations
			long cumulatedCount = COUNT.incrementAndGet();
			if (Long.bitCount(cumulatedCount) == 1) {
				LOGGER.info("For Greeks={} and aeAfCeCf={} ({})", greeks, aeAfCeCf, cumulatedCount);
			}
			// }

		});

		return greeksToCombinations;
	}

	static boolean checkAEZeroesIncompatibility3(AE4 af, AE4 ce, AE4 ae) {
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
