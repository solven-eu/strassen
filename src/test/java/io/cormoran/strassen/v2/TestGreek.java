package io.cormoran.strassen.v2;

import org.junit.Assert;
import org.junit.Test;

public class TestGreek {
	@Test
	public void testSoftlyGrowing() {
		// -1 everywhere
		Assert.assertTrue(Greek.isSoftlyGrowing(new Greek(0)));

		// 0 then -1 everywhere
		Assert.assertFalse(Greek.isSoftlyGrowing(new Greek(1)));
	}

	@Test
	public void testComparision() {

		// 2019-03-05 09:54:23,542 [main] (INFO) io.cormoran.strassen.v2.RunStrassen.lambda$45(374) - For Alpha=~0 ~0 +1 +1 +1 Delta=-1 +1 ~0 ~0 ~0 Beta=+1 -1 +1 ~0 +1 Gamma=-1 +1 +1 +1 ~0 and AE=-1 -1 -1 +1 +1 and AF=~0 ~0 +1 -1~0 and CE=~0 ~0 +1 ~0 -1, CF=~0 +1 ~0 -1 +1 (131072)
		// 2019-03-05 09:54:25,580 [main] (INFO) io.cormoran.strassen.v2.RunStrassen.lambda$45(374) - For Alpha=~0 ~0 +1 +1 +1 Delta=~0 +1 -1 -1 -1 Beta=~0 ~0 -1 ~0 -1 Gamma=-1 -1 +1 +1 +1 and AE=~0 +1 -1 +1 +1 and AF=~0 ~0 -1 +1~0 and CE=-1 ~0 ~0 ~0 ~0, CF=-1 +1 -1 ~0 +1 (262144)
		// 2019-03-05 09:54:29,524 [main] (INFO) io.cormoran.strassen.v2.RunStrassen.lambda$45(374) - For Alpha=~0 ~0 +1 +1 +1 Delta=~0 +1 ~0 +1 +1 Beta=+1 ~0 +1 ~0 +1 Gamma=-1 ~0 -1 -1 -1 and AE=-1 -1 ~0 ~0 +1 and AF=~0 ~0 ~0 -1+1 and CE=-1 +1 +1 -1 ~0, CF=~0 +1 ~0 ~0 ~0 (524288)
		{
		int[] before = new int[] {0, 0, 1, 1, 1};
		int[] after = new int[] {-1,1,0,0,0};
		Assert.assertEquals(1,
				new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, before))
						.compareTo(new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE,after))));
		}
		{
			int[] before = new int[] {0, 0, 1, 1, 1};
			int[] after = new int[] {0,1,-1,-1,-1};
			Assert.assertEquals(1,
					new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE, before))
							.compareTo(new Greek(Vector.computeIndex(Greek.NB_COEF, Greek.MAX_VALUE,after))));	
		}
	}
}
