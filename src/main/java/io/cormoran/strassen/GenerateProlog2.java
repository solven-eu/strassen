package io.cormoran.strassen;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class GenerateProlog2 {
	public static void main(String[] args) {
		List<String> l1234 = Arrays.asList("1", "2", "3", "4");
		List<String> ijkl = Arrays.asList("I", "J", "K", "L");

		Set<List<String>> pairs =
				Sets.cartesianProduct(ImmutableSet.of("A", "B", "C", "D"), ImmutableSet.of("E", "F", "G", "H"));
		pairs.forEach(pair -> {
			for (int j = 0; j < 4; j++) {
				System.out.print("is_zero(");
				for (int i = 0; i < 4; i++) {
					// I1
					System.out.print(ijkl.get(i));
					System.out.print(l1234.get(j));

					System.out.print(",");

					System.out.print(pair.get(0));
					System.out.print(l1234.get(i));

					System.out.print(",");

					System.out.print(pair.get(1));
					System.out.print(l1234.get(i));

					if (i < 3) {
						System.out.print(",");
					} else {
						System.out.print("),");
					}
				}
				System.out.println();
			}
			System.out.println();
		});
	}
}
