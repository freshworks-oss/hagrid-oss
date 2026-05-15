package com.freshworks.freshindex.index.query;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonDeserialize(using = ExpressionDeserializer.class)
public class Expression {

    enum JOIN {

        AND,

        OR
    }
    String currentExpressionStr;
    Expression leftExpression;

    Expression rightExpression;

    JOIN leftRightExpressionOperator;


    public static ExpressionBuilder expressionBuilder(){

        return new ExpressionBuilder();
    }


    public static ExpressionJoiner expressionJoiner(){

        return new ExpressionJoiner();
    }

    public static class ExpressionJoiner{

        Expression whenLeftExpressionIs;

        Expression whenRightExpressionIs;

        JOIN whenJoinerIs;


        public ExpressionJoiner whenLeftExpressionIs(Expression leftExpression){

            this.whenLeftExpressionIs = leftExpression;
            return this;
        }

        public ExpressionJoiner whenRightExpressionIs(Expression whenRightExpressionIs){
            this.whenRightExpressionIs = whenRightExpressionIs;
            return this;
        }

        public ExpressionJoiner whenJoinerIsAnd(){
            this.whenJoinerIs = JOIN.AND;
            return this;
        }

        public ExpressionJoiner whenJoinerIsOR(){
            this.whenJoinerIs = JOIN.OR;
            return this;
        }


        public Expression build(){

            Expression expression = new Expression();
            expression.currentExpressionStr = "(" + whenLeftExpressionIs.currentExpressionStr + " " + whenJoinerIs.toString() + " " + whenRightExpressionIs.currentExpressionStr + ")";
            expression.leftRightExpressionOperator = whenJoinerIs;
            expression.leftExpression = whenLeftExpressionIs;
            expression.rightExpression = whenRightExpressionIs;
            return expression;
        }
    }


    public static class ExpressionBuilder {
        String whenAssetFieldName;

        String operator;

        String whenAssetFieldValue;


        public ExpressionBuilder whenAssetFieldName(String whenAssetFieldName){
            this.whenAssetFieldName = whenAssetFieldName;
            return this;
        }

        public ExpressionBuilder is(){
            operator = "=";
            return this;
        }

        public ExpressionBuilder isNot(){
            operator = "!=";
            return this;
        }

        public ExpressionBuilder greaterThan(){
            operator = ">";
            return  this;
        }

        public ExpressionBuilder lessThan(){
            operator = "<";
            return  this;
        }

        public ExpressionBuilder whenAssetFieldValue(String whenAssetFieldValue){

            this.whenAssetFieldValue = whenAssetFieldValue;
            return this;
        }

        public Expression build(){

            Expression expression = new Expression();
            String expressionStr =  "(" + this.whenAssetFieldName + " " + this.operator +  " " + this.whenAssetFieldValue + ")";
            expression.setCurrentExpressionStr(expressionStr);
            expression.leftExpression = null;
            expression.rightExpression = null;
            expression.leftRightExpressionOperator = null;
            return expression;
        }
    }

}
