package com.freshworks.freshindex.index.query;

import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.SummaryIndex;
import com.freshworks.freshindex.index.typeindex.BaseIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
@Slf4j
public class JsonQueryService {

    SummaryIndex summaryIndex;

    NamespaceService namespaceService;

    @Autowired
    public JsonQueryService(NamespaceService namespaceService){
        this.namespaceService = namespaceService;
    }

    public void configure(String namespace){

        if(namespaceService.containsNamespace(namespace)){
            this.summaryIndex = namespaceService.getSummaryIndexByNamespace(namespace);
        }
        else {

            // Do not do anything as we are initilizing new summary index already.
            this.summaryIndex = getSummaryIndex();
            namespaceService.setSummaryIndexByNamespace(namespace, summaryIndex);
        }
    }

    public List<String> queryAssetByExpression(Expression expression) throws Exception{

        return expressionTraverser(expression);
    }


    private List<String> expressionTraverser(Expression expression) throws Exception{

        if(expression.leftExpression != null || expression.rightExpression != null){

            List<String> leftExpressionDocIds = expressionTraverser(expression.leftExpression);
            List<String> rightExpressionDocIds = expressionTraverser(expression.rightExpression);

            if(expression.leftRightExpressionOperator == Expression.JOIN.AND){
                List<String> common = new ArrayList<>(leftExpressionDocIds);
                common.retainAll(rightExpressionDocIds);
                return common;
            }

            else if (expression.leftRightExpressionOperator == Expression.JOIN.OR){
                List<String> common = new ArrayList<>(leftExpressionDocIds);
                common.removeAll(rightExpressionDocIds);
                common.addAll(rightExpressionDocIds);
                return common;
            }

            else{
                return new ArrayList<>();
            }
        }

        else{
            String currentExpressionStr = expression.currentExpressionStr;

            Pattern pattern = Pattern.compile("\\((.*)[ ]+([><!==])+[ ]+(.*)\\)");
            Matcher matcher = pattern.matcher(currentExpressionStr);

            String lhs;
            String operator;
            String rhs;

            if(matcher.matches()){
                lhs = matcher.group(1);
                operator = matcher.group(2);
                rhs = matcher.group(3);
            }
            else{
                throw  new Exception("Could not extract lhs, operator or rhs");
            }

            return queryAsset(lhs, operator, rhs);
        }
    }


    public List<String> queryAsset(String query, String operator, String value) throws Exception{

        BaseIndex baseIndex = this.summaryIndex.getBaseIndex(query);

        if(baseIndex == null){
            return new ArrayList<>();
        }
        List<String> docs = baseIndex.get(value, operator);

        if(docs == null){
            return new ArrayList<String>();
        }
        else{
            return docs;
        }
    }

    public List<Object> query(String query) throws Exception{

        BaseIndex baseIndex = this.summaryIndex.getBaseIndex(query);
        List<Object> docs = baseIndex.getValues();

        if(docs == null){
            return new ArrayList<Object>();
        }
        else{
            return docs;
        }
    }

    @Lookup
    public SummaryIndex getSummaryIndex(){

        return  null;
    }
}
