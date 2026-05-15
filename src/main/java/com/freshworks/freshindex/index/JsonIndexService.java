package com.freshworks.freshindex.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.typeindex.BaseIndex;
import com.freshworks.freshindex.index.typeindex.DoubleIndex;
import com.freshworks.freshindex.index.typeindex.StringIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.naming.Name;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Scope("prototype")
public class JsonIndexService {

    SummaryIndex summaryIndex;

    NamespaceService namespaceService;

    ReentrantReadWriteLock.WriteLock uniqueInsertLock = new ReentrantReadWriteLock().writeLock();

    @Autowired
    public JsonIndexService(NamespaceService namespaceService){
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

    enum INDEX_TYPE{
        STRING,
        INTEGER
    }


    public void updateJsonString(JsonNode oldJsonNode, JsonNode newJsonNode, String documentId) throws Exception {

        try{
            uniqueInsertLock.lock();
            Map<String, JsonNode> toBeIndexTokens;

            // First delete the old json string
            deleteJsonString(oldJsonNode, documentId);

            // Then insert the new one
            toBeIndexTokens = tokenize(newJsonNode, documentId);
            insertTokens(toBeIndexTokens, documentId);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            uniqueInsertLock.unlock();
        }

    }


    public void deleteJsonString(JsonNode oldJsonNode, String documentId) throws Exception {

        Map<String, JsonNode> toBeIndexTokens;

        // First remove the old tokens
        toBeIndexTokens = tokenize(oldJsonNode, documentId);
        removeTokens(toBeIndexTokens, documentId);
    }

    public void indexJsonString(JsonNode inputJsonNode, String documentId) throws Exception {

        try{
            uniqueInsertLock.lock();
            Map<String, JsonNode> toBeIndexTokens;
            toBeIndexTokens = tokenize(inputJsonNode, documentId);
            insertTokens(toBeIndexTokens, documentId);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            uniqueInsertLock.unlock();
        }

    }

    public void indexJsonStringBulk(List<JsonNode> inputJsonNodeList, List<String> documentIdList) throws Exception {

        try{
            uniqueInsertLock.lock();

            Map<String, JsonNode> toBeIndexTokens;
            for(int i=0 ;i<inputJsonNodeList.size(); i++){
                toBeIndexTokens = tokenize(inputJsonNodeList.get(i), documentIdList.get(i));
                insertTokens(toBeIndexTokens, documentIdList.get(i));
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            uniqueInsertLock.unlock();
        }

    }


    public Map<String, JsonNode> tokenize(JsonNode inputJsonNode, String documentId) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, JsonNode> toBeIndexTokens = new HashMap<>();

        if(inputJsonNode.isArray()){

            for (JsonNode eachJson: inputJsonNode) {
                 toBeIndexTokens = parse("$", eachJson, new HashMap<>());
            }
        }
        else{
            toBeIndexTokens = parse("$", inputJsonNode, new HashMap<>());
        }


        return  toBeIndexTokens;
    }


    private Map<String, JsonNode> parse( String path, JsonNode currentJsonNode, Map<String, JsonNode> result) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = currentJsonNode;

        Iterator<Map.Entry<String, JsonNode>> jsonEntryIterator = json.fields();

        while(jsonEntryIterator.hasNext()){

            Map.Entry<String, JsonNode> entry = jsonEntryIterator.next();
            JsonNode value  = entry.getValue();

            if(value.isArray()){

                int counter = 0;
                boolean isArrayItemsPrimitive = false;
                for (JsonNode item: value) {

                    if(item.isObject()){

                        // Keep on recursive if item of the array is json. It is the case of array of json
                        parse(path + "." + entry.getKey() + "." + counter, objectMapper.readTree(item.toString()), result);
                        parse(path + "." + entry.getKey() + "." + "*", objectMapper.readTree(item.toString()), result);
                    }

                    else {
                        // If the item is primitive type then put it in result. $.students.friends.1 = "A", $.students.friends.2 = "B"
                        result.put(path + "." + entry.getKey() + "." + counter, item);
                        isArrayItemsPrimitive = true;
                    }

                    counter = counter + 1;
                }

//                // Here we are putting whole json as it is to support wild card $.students.friends.* = ["A","B", "C"]
                if(isArrayItemsPrimitive){
                    result.put(path + "." + entry.getKey() + "." + "*", entry.getValue());
                }
                else{
                    // Do not put whole json array as its items are not primitive.
                }

            }


            else if (value.isObject()){

                // Keep on recursive until value is json object
                parse(path + "." + entry.getKey(), objectMapper.readTree(value.toString()), result);
            }

            else{


                // Here check if for the exact same path, if key already exists, if so then convert it into ArrayJson Node and insert it
                if(result.containsKey(path + "." + entry.getKey())){
                    JsonNode jsonNode = result.get(path + "." + entry.getKey());
                    ArrayNode arrayNode = (ArrayNode) jsonNode;
                    arrayNode.add(value);
                    result.put(path + "." + entry.getKey(), arrayNode);
                }

                else{
                    ArrayNode arrayNode = objectMapper.createArrayNode();
                    arrayNode.add(value);
                    result.put(path + "." + entry.getKey(), arrayNode);
                }

            }
        }

        return result;
    }


    private void insertTokens(Map<String, JsonNode> toBeIndexTokens, String documentId) throws Exception{

        try{

            for (Map.Entry<String, JsonNode> token: toBeIndexTokens.entrySet()) {

                if(summaryIndex.containsBaseKey(token.getKey())){

                    // Here get index for this token
                    BaseIndex baseIndex = summaryIndex.getBaseIndex(token.getKey());
                    JsonNode value = token.getValue();
                    insert(baseIndex, value, documentId);
                }
                else{

                    // check if value is array
                    JsonNode value = token.getValue();
                    INDEX_TYPE indexType = getIndexType(value);

                    if(indexType == INDEX_TYPE.STRING){

                        StringIndex stringIndex = getStringIndex();
                        insert(stringIndex, token.getValue(), documentId);
                        summaryIndex.putBaseIndex(token.getKey(), stringIndex);
                    }
                    else{

                        DoubleIndex doubleIndex = getDoubleIndex();
                        insert(doubleIndex, token.getValue(), documentId);
                        summaryIndex.putBaseIndex(token.getKey(), doubleIndex);
                    }

                }
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
        }
    }

    private void insert(BaseIndex baseIndex, JsonNode value, String documentId) throws Exception{

        if(value.isArray()){

            for (JsonNode primitive: value) {
                baseIndex.insert(primitive.asText(), documentId);
            }
        }
        else{
            baseIndex.insert(value.asText(), documentId);
        }

    }

    private INDEX_TYPE getIndexType(JsonNode value){

        Boolean isString = false;

        if(value.isArray()){

            for (JsonNode primitive: value) {

                if(isNumber(primitive.asText())){
                    isString = false;
                }
                else{
                    isString = true;
                    break;
                }
            }
        }

        else if(isNumber(value.asText())){
            isString = false;
        }
        else{
            isString = true;
        }

        if(isString){

            return INDEX_TYPE.STRING;
        }
        else{
            return INDEX_TYPE.INTEGER;
        }
    }

    private boolean isNumber(String s){

        Pattern pattern = Pattern.compile("^\\d+\\.?\\d*");
        Matcher matcher = pattern.matcher(s);

        if(matcher.matches()){
            return true;
        }
        else{
            return false;
        }
    }

    private void removeTokens(Map<String, JsonNode> toBeIndexTokens, String documentId) throws Exception{

        for (Map.Entry<String, JsonNode> token: toBeIndexTokens.entrySet()) {

            if(summaryIndex.containsBaseKey(token.getKey())){

                // Here get index for this token
                BaseIndex baseIndex = summaryIndex.getBaseIndex(token.getKey());
                JsonNode value = token.getValue();
                remove(baseIndex, value, documentId);
            }
            else{
                log.warn("Token {} does not exists in the index. Hence can not remove it", token.getKey());
            }
        }
    }

    private void remove(BaseIndex baseIndex, JsonNode value, String documentId) throws Exception{

        if(value.isArray()){

            for (JsonNode primitive: value) {
                baseIndex.remove(primitive.asText(), documentId);
            }
        }
        else{
            baseIndex.remove(value.asText(), documentId);
        }

    }

    public boolean isNonNegativeNumber(String str) {
        return str.matches("\\d+");
    }


    @Lookup
    public StringIndex getStringIndex(){

        return  null;
    }

    @Lookup
    public DoubleIndex getDoubleIndex(){

        return  null;
    }

    @Lookup
    public SummaryIndex getSummaryIndex(){

        return  null;
    }
}
