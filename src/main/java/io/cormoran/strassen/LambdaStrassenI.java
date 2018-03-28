package io.cormoran.strassen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.SetMultimap;
import com.google.common.io.CharStreams;

/**
 * mvn package shade:shade lambda:deploy-lambda
 * 
 * @author Benoit Lacelle
 *
 */
public class LambdaStrassenI extends LambdaStrassen {

	static final LoadingCache<StrassenLimits, SetMultimap<V5, List<V5>>> preparedPairs =
			CacheBuilder.newBuilder().build(CacheLoader.from(limits -> new Strassen(limits).preparedPairs()));

	static final LoadingCache<StrassenLimits, SetMultimap<V5, V5>> leftToRightGiving0 =
			CacheBuilder.newBuilder().build(CacheLoader.from(limits -> new Strassen(limits).leftToRightFor0()));
	static final LoadingCache<StrassenLimits, SetMultimap<V5, V5>> leftToRightGiving1 =
			CacheBuilder.newBuilder().build(CacheLoader.from(limits -> new Strassen(limits).leftToRightGiving1()));

	// Required by AWS Lambda as .handleRequest is not static
	// https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html
	public LambdaStrassenI() {
	}

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
			throws JsonProcessingException, IOException {
		String inputAsString = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

		context.getLogger().log("Received as command: " + inputAsString);

		QueryI ijkl = objectMapper.readValue(inputAsString, QueryI.class);

		List<AnswerIJKLABCDEFGH> count = countForIJK(ijkl)
				.map(list -> new AnswerIJKLABCDEFGH(new QueryIJKL(list.get(0), list.get(1), list.get(2), list.get(3)),
						new QueryIJKL(list.get(4), list.get(5), list.get(6), list.get(7)),
						new QueryIJKL(list.get(8), list.get(9), list.get(10), list.get(11))))
				.collect(Collectors.toList());

		objectMapper.writer().writeValue(outputStream, count);
	}

	public Stream<List<V5>> countForIJK(QueryI indexI) {
		// Stream<IJKLAndAEs> stream = Strassen.allIJKLAsStream(LambdaStrassen.leftToRightGiving0,
		// LambdaStrassen.leftToRightGiving1,
		// Stream.of(indexI.i),
		// Optional.empty(),
		// Optional.empty()).parallel();
		//
		// return stream.flatMap(ijkl -> Strassen.processIJKL(leftToRightGiving0,
		// leftToRightGiving1,
		// // aeToAE,
		// preparedPairs,
		// ijkl.aeCandidates,
		// ijkl.ijkl));
		return Stream.empty();
	}

}
