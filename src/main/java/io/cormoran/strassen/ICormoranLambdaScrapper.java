package io.cormoran.strassen;

import java.util.List;

import com.amazonaws.services.lambda.invoke.LambdaFunction;
import com.amazonaws.services.lambda.model.LogType;

public interface ICormoranLambdaScrapper {

	@LambdaFunction(functionName = "strassenIjkldev")
	List<AnswerIJKLABCDEFGH> countCats(QueryIJKL input);

	@LambdaFunction(functionName = "strassenIjkldev", logType = LogType.Tail)
	List<AnswerIJKLABCDEFGH> countCatsWithLogs(QueryIJKL input);

	@LambdaFunction(functionName = "strassenIjkdev")
	List<AnswerIJKLABCDEFGH> countCats(QueryIJK input);

}