package io.cormoran.strassen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.CharStreams;

/**
 * mvn package shade:shade lambda:deploy-lambda
 * 
 * @author Benoit Lacelle
 *
 */
public class LambdaStrassen implements RequestStreamHandler {
	protected static final Logger LOGGER = LoggerFactory.getLogger(LambdaStrassen.class);

	protected static ObjectMapper objectMapper = new ObjectMapper();

	public static class LambdaScrapperSpringConfig {

	}

	// Required by AWS Lambda as .handleRequest is not static
	// https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html
	public LambdaStrassen() {
	}

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
			throws JsonProcessingException, IOException {
		String inputAsString = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

		context.getLogger().log("Received as command: " + inputAsString);

		QueryIJKL ijkl = objectMapper.readValue(inputAsString, QueryIJKL.class);

		throw new UnsupportedOperationException();
	}

}
