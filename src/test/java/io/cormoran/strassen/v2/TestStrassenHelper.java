package io.cormoran.strassen.v2;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestStrassenHelper {
	StrassenHelper helper = new StrassenHelper();

	@Test
	public void testContainsOnlyOneAndOnlyMinusOne() {
		Set<AE4> allAEs = helper.allAEs;
		Assert.assertTrue(allAEs.contains(new AE4(Vector.computeIndex(AE4.NB_MUL, AE4.MAX_AE, 1, 1, 1, 1, 1))));
		Assert.assertTrue(allAEs.contains(new AE4(Vector.computeIndex(AE4.NB_MUL, AE4.MAX_AE, -1, -1, -1, -1, -1))));
	}

	@Test
	public void testSymmetryWithNegatives_1() {
		Set<AE4> valuesForPlusOne =
				helper.gives1(new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, 1, 1, 1, 1, 1)));
		Set<AE4> valuesForMinusOne =
				helper.gives1(new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, -1, -1, -1, -1, -1)));

		// It is easier to get to 1 from -1 than from 1???
		Assert.assertEquals(valuesForPlusOne.size(), valuesForMinusOne.size());
	}

	@Test
	public void testSymmetryWithNegatives_0() {
		Set<AE4> valuesForPlusOne =
				helper.gives0(new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, 1, 1, 1, 1, 1)));
		Set<AE4> valuesForMinusOne =
				helper.gives0(new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, -1, -1, -1, -1, -1)));

		Assert.assertEquals(valuesForPlusOne.size(), valuesForMinusOne.size());
	}
}
