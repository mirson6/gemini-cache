package com.mirson.gemini.cache.utils;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * Spring EL 表达式处理工具
 */
@Component
public class SpringExpressionParserUtil {

  protected static class RootObject {

    private final Object[] args;

    private RootObject(Object[] args) {
      super();
      this.args = args;
    }

    public Object[] getArgs() {
      return args;
    }
  }

  public Object parseAndGetCacheKeyFromExpression(String input,
      final Object returnedObject,
      Object[] args, KeyGenerators keyGenerator) {

    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext standardEvaluationContext;
    input = input.trim();
    List<Object> params = new LinkedList<>();
    if (input.startsWith("#result")) {
      Expression exp = parser.parseExpression(input);
      standardEvaluationContext = new StandardEvaluationContext();
      standardEvaluationContext.setVariable("result", returnedObject);
      params.add(exp.getValue(standardEvaluationContext, Object.class));
    } else if (input.startsWith("#param")) {
      String[] paramsArr = input.split(",");
      for (String param : paramsArr) {
        standardEvaluationContext = new StandardEvaluationContext(new RootObject(args));
        Expression expressionObj = parser
            .parseExpression(convertAnnotationInputToSpringExpression(param));
        params.add(expressionObj.getValue(standardEvaluationContext, Object.class));
      }
    } else {
      throw new IllegalArgumentException("Invalid key expression");
    }
    return getCacheKey(params, keyGenerator);
  }

  private Object getCacheKey(List<Object> params, KeyGenerators keyGenerator) {
    switch (keyGenerator) {
    case SHA:
      return CacheUtil.buildCacheKey(params);
    case CONCAT:
      return CacheUtil.buildStringCacheKey(params.toArray());
    default:
      return CacheUtil.buildCacheKey(params);
    }
  }

  private String convertAnnotationInputToSpringExpression(String inputString) {
    inputString = inputString.trim();
    String[] inputStringArr = inputString.split("\\.");
    String indexNumberStr = inputStringArr[0].replace("#param", "");
    int indexNumber = Integer.valueOf(indexNumberStr);
    indexNumber--;
    if (inputStringArr.length > 1)
      return "args[" + indexNumber + "]" + "." + inputStringArr[1];
    else
      return "args[" + indexNumber + "]";
  }

}
