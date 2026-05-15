package com.freshworks.freshindex.index.query;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionDeserializer extends StdDeserializer<Expression> {

    public ExpressionDeserializer(){
        this(null);

    }
    protected ExpressionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Expression deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String expressionStr = node.asText();
        Expression expression = new Expression();
        buildExpressionFromString(expressionStr, expression);
        return expression;
    }

    public void buildExpressionFromString(String expressionStr, Expression rootExpression){

        rootExpression.setCurrentExpressionStr(expressionStr);

        String leftExpressionStr;
        String rightExpressionStr;
        String operatorStr;

        Pattern pattern = Pattern.compile("\\((.*)[ ]+(and|AND|or|OR)[ ]+(.*)\\)");
        Matcher matcher = pattern.matcher(expressionStr);

        if(matcher.matches()){
            leftExpressionStr = matcher.group(1);
            operatorStr = matcher.group(2);
            rightExpressionStr = matcher.group(3);

            Expression leftExpression = new Expression();
            leftExpression.setCurrentExpressionStr(leftExpressionStr);
            leftExpression.setRightExpression(null);
            leftExpression.setLeftExpression(null);
            leftExpression.setLeftRightExpressionOperator(null);

            Expression rightExpression = new Expression();
            rightExpression.setCurrentExpressionStr(rightExpressionStr);
            rightExpression.setRightExpression(null);
            rightExpression.setLeftExpression(null);
            rightExpression.setLeftRightExpressionOperator(null);

            rootExpression.setLeftExpression(leftExpression);
            rootExpression.setRightExpression(rightExpression);
            rootExpression.setLeftRightExpressionOperator(Expression.JOIN.valueOf(operatorStr.toUpperCase()));
            buildExpressionFromString(leftExpressionStr, leftExpression);
            buildExpressionFromString(rightExpressionStr, rightExpression);
        }
    }
}
