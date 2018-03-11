package io.cormoran.strassen;

import java.util.Map;

import com.amazonaws.services.lambda.invoke.LambdaFunction;
import com.amazonaws.services.lambda.model.LogType;

public interface ICormoranLambdaScrapper {

	@LambdaFunction(functionName = "strassenIjkl")
	Map<?, ?> countCats(QueryIJKL input);

	@LambdaFunction(functionName = "strassenIjkl", logType = LogType.Tail)
	Map<?, ?> countCatsWithLogs(QueryIJKL input);

}