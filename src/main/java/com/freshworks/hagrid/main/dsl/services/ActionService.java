package com.freshworks.hagrid.main.dsl.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.traverser.DagTraversalService;
import com.freshworks.hagrid.traverser.RequestResponseContainer;
import com.freshworks.hagrid.traverser.exception.StepFailedException;
import com.freshworks.hagrid.main.dsl.config.action.ActionConfig;
import com.freshworks.hagrid.main.dsl.config.action.ActionSpec;
import com.freshworks.hagrid.main.dsl.config.action.CompositeActionConfig;
import com.freshworks.hagrid.main.dsl.config.model.ApiModelConfig;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.model.OutputModelConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;
import com.freshworks.hagrid.main.dsl.runnable.Hooks;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel.Join;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext.ParseSyncResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;
import com.freshworks.hagrid.main.dsl.services.resolvers.ActionResolver;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;

@Component
@Scope("prototype")
public class ActionService {
    
    // Variables that should be made available in DSL while execution 
    Map<String, Object> actionInput;
    List<Map<String, Object>> parentModelData = new ArrayList<>();

    ActionContext actionContext = new ActionContext();

    ActionConfig actionConfig;
    ActionSpec actionSpec;
    ActionServiceUtility actionServiceUtility;

    ActionResolver actionResolver;

    ModelSpec modelSpec;

    RequestService requestService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ActionService(ActionSpec actionSpec, ModelSpec modelSpec, ActionResolver actionResolver, RequestService requestService, ActionServiceUtility actionServiceUtility){
        this.actionSpec = actionSpec;
        this.modelSpec = modelSpec;
        this.requestService = requestService;
        this.actionServiceUtility = actionServiceUtility;
        this.actionResolver = actionResolver;
    }


    /**
     * This will be configured when action is not top level action
     * @param actionName
     * @param parentActionName
     */
    public void configure(String actionName, Map<String, Object> actionInput, JsonNode... parentDataList){

        CompositeActionConfig compositeActionConfig = this.actionSpec.getCompositeActionByName(actionName);
        this.actionConfig = compositeActionConfig.getActionByName(actionName);
        this.actionInput = actionInput;

        for(JsonNode parentData : parentDataList){

            if(parentData.has("data")){
                JsonNode apiModelNode = parentData.get("data");
                ApiModel apiModel = objectMapper.convertValue(apiModelNode, ApiModel.class);
                this.parentModelData.add(apiModel.getModelDataAsMap());
            }
        }

        actionContext.setInput(this.actionInput);
        actionContext.setParentApiModelData(parentModelData);
        actionContext.setActionSharedMap(new HashMap<>());
    }

    public void setup(JsonNode... parentJsonObject){

        /**
        *  Call this hook to allow developer to set up 
        */
        
        try{

            Hooks hook = this.actionResolver.resolveHook(this.actionConfig.getHooksConfig(), null);
            Closure setupClosure = hook.getSetupClosure();

            if(setupClosure == null){
                // Do not do anything
            }
            else{
                hook.call_setupClosure(this.actionContext);
            }

        }

        catch(Exception e){
            e.printStackTrace();
        }

    }

    public Boolean shouldProceedWithParent(JsonNode... parentJsonObject) throws StepFailedException{

        /**
        * Call this hook to check whether should we skip this parent of continue
        */
        
        try{

            Hooks hook = this.actionResolver.resolveHook(this.actionConfig.getHooksConfig(), null);
            Closure shouldSkipThisParentClosure = hook.getShouldSkipThisParentClosure();

            if(shouldSkipThisParentClosure == null){
                return true;
            }

            // Here I am negating the value return because hagrid need whether should proceed, however config returns should it skip
            return !hook.call_shouldSkipThisParent(this.actionContext);
        }

        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


    public RequestResponseContainer startAction(JsonNode... parentJsonObject) throws Exception{

        RequestConfig requestConfig = actionConfig.getActionRequestConfig().getRequestConfig();
        LinkedHashMap<String, Closure> actionRequestMapping = actionConfig.getActionRequestConfig().getMapping();

        // Now resolve all closures
        LinkedHashMap<String, Object> resolvedContext = new LinkedHashMap<>();

        // Here , i am resolving the context first, before resolving the requests
        for(Map.Entry<String, Closure> c : actionRequestMapping.entrySet()){

            c.getValue().setDelegate(this.actionContext);
            c.getValue().setResolveStrategy(Closure.DELEGATE_FIRST);
            String resolvedParam = String.valueOf(c.getValue().call());

            resolvedContext.put(c.getKey(), resolvedParam);
        }

        this.actionContext.getActionSharedMap().put("_resolved_request_params", resolvedContext);
        ActionRequest request = this.actionResolver.resolveRequest(requestConfig, this.actionContext);
        this.actionContext.getActionSharedMap().remove("_resolved_request_params");

        RequestResponseContainer requestResponseContainer = new RequestResponseContainer();
        requestResponseContainer.setRequest(request);
        return requestResponseContainer;
    }


    public RequestResponseContainer execute(RequestResponseContainer requestResponseContainer, JsonNode... parentActionData) {
        try{

            Hooks hook = this.actionResolver.resolveHook(this.actionConfig.getHooksConfig(), null);
            Closure executeClosure = hook.getExecuteClosure();

            if(executeClosure == null){
                
                ActionRequest request = (ActionRequest)requestResponseContainer.getRequest();
                ActionResponse actionResponse = this.requestService.execute(request);
                requestResponseContainer.setResponse(actionResponse);
                return requestResponseContainer;

            }

            ActionRequest r = objectMapper.convertValue(requestResponseContainer.getRequest(), ActionRequest.class);
            this.actionContext.setActionRequest(r);
            
            ActionResponse actionResponse = hook.call_executeClosure(actionContext);
            requestResponseContainer.setResponse(actionResponse);
            return requestResponseContainer;
        }

        catch(Exception e){

            e.printStackTrace();
            return null;
        }
    }

    public boolean isValidResponse(RequestResponseContainer requestResponseContainer, JsonNode... parentActionData){
        
        try{

            Hooks hook = this.actionResolver.resolveHook(this.actionConfig.getHooksConfig(), null);
            Closure shouldPassThroughResponse = hook.getShoudlPassthroughResponseClosure();

            ActionResponse response = (ActionResponse)requestResponseContainer.getResponse();
        
            if(shouldPassThroughResponse == null){
            
                if(response.getHttpStatus().equals(ACTION_HTTP_CODE.OK)){

                    return true;
                }

                else{
                    return false;
                }
            }

            ActionRequest r = objectMapper.convertValue(requestResponseContainer.getRequest(), ActionRequest.class);
            ActionResponse actionResponse = objectMapper.convertValue(requestResponseContainer.getResponse(), ActionResponse.class);
            this.actionContext.setActionRequest(r);
            this.actionContext.setActionResponse(actionResponse);

            return hook.call_shouldPassthroughResponse(actionContext);
        }

        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }


    public DagTraversalService.TraverseAction handleInValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentActionData){

        return null;
    }


    public RequestResponseContainer getNextAction(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception{


        RequestConfig paginateRequestConfig = actionConfig.getPaginateRequestConfig().getRequestConfig();
        LinkedHashMap<String, Closure> actionRequestMapping = actionConfig.getPaginateRequestConfig().getMapping();

        // Now resolve all closures
        LinkedHashMap<String, Object> resolvedContext = new LinkedHashMap<>();
        ActionRequest r = objectMapper.convertValue(currentRequest.getRequest(), ActionRequest.class);
        ActionResponse actionResponse = objectMapper.convertValue(currentRequest.getResponse(), ActionResponse.class); 
        
        this.actionContext.setActionRequest(r);
        this.actionContext.setActionResponse(actionResponse);

        for(Map.Entry<String, Closure> c : actionRequestMapping.entrySet()){

            c.getValue().setDelegate(actionContext);
            c.getValue().setResolveStrategy(Closure.DELEGATE_ONLY);
            Object resolvedParam = c.getValue().call();

            resolvedContext.put(c.getKey(), resolvedParam);
        }

        this.actionContext.getActionSharedMap().put("_resolved_request_params", resolvedContext);
        ActionRequest paginatedRequest = this.actionResolver.resolveRequest(paginateRequestConfig, this.actionContext);
        this.actionContext.getActionSharedMap().remove("_resolved_request_params");
        
        RequestResponseContainer requestResponseContainer = new RequestResponseContainer();
        requestResponseContainer.setRequest(paginatedRequest);
        

        return requestResponseContainer;
    }

    public JsonNode parseSyncResponseNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject){

        ArrayNode filteredResponse = objectMapper.createArrayNode();
        
        try{

            ActionRequest r = objectMapper.convertValue(currentRequestResponse.getRequest(), ActionRequest.class);
            ActionResponse actionResponse = objectMapper.convertValue(currentRequestResponse.getResponse(), ActionResponse.class); 
            
            this.actionContext.setActionRequest(r);
            this.actionContext.setActionResponse(actionResponse);
            
            // Map context = createContext(this.context, r, res.getResponse().get("body"));

            // Now check if hooks is provided for this 
            
            Hooks hook = this.actionResolver.resolveHook(this.actionConfig.getHooksConfig(), this.actionContext);
            Closure parseSyncResponseClosure = hook.getParseSyncResponseClosure();

            ParseSyncResponse parseSyncResponseObject = null;

            // It means that developer want to handle custom errors
            if(parseSyncResponseClosure != null){

                parseSyncResponseObject = hook.call_parseSyncResponseClosure(actionContext);

            }

            JsonNode diggedResponseBody = null;
            ApiModelConfig apiModelConfig = null;
            if(parseSyncResponseObject == null){

                // First resolve response Path here 
                Closure c = this.actionConfig.getResponsePathClosure();
                c.setDelegate(actionContext);
                c.setResolveStrategy(Closure.DELEGATE_ONLY);
                diggedResponseBody = objectMapper.convertValue(c.call(), JsonNode.class);
                apiModelConfig = actionConfig.getApiModelConfig();
            }

            else{

                // Parse response body here manually
                diggedResponseBody = parseSyncResponseObject.getDiggedResponseBody();
                // Here using modelSpec take the apiModelConfig 
                apiModelConfig = this.modelSpec.getApiModelByName(parseSyncResponseObject.getModelName());

                this.actionContext.getActionSharedMap().put("_custom_output_model", parseSyncResponseObject.getOutputModelName());
                this.actionContext.getActionSharedMap().put("_response_type", parseSyncResponseObject.getResponseType());
            }
            

            // Now resolve model attributes
            Map<String, Closure> desiredFieldListConfig = apiModelConfig.getDesiredAttrListMap();

            if(diggedResponseBody.isArray()){

                // I am looping through each model in the response 
                for(JsonNode eachModel: diggedResponseBody){

                    // Creating a context for each

                    
                    Map resolvedApiModelAttributeMap = new HashMap<>();

                    // Create a context 
                    Map eachModelMap = objectMapper.convertValue(eachModel, Map.class);
                    actionResponse = this.actionContext.getActionResponse();
                    actionResponse.setModel(eachModelMap);                    

                    for(Map.Entry<String, Closure> fieldConfig: desiredFieldListConfig.entrySet()){    

                        Closure c = fieldConfig.getValue();

                        // resolve field config closure on api response
                        c.setDelegate(this.actionContext);
                        c.setResolveStrategy(Closure.DELEGATE_ONLY);
                        resolvedApiModelAttributeMap.put(fieldConfig.getKey(), c.call());
                    }

                    ApiModel apiModel = new ApiModel();
                    apiModel.setName(apiModelConfig.getName());
                    apiModel.setFilterClosure(apiModelConfig.getFilterClosure());
                    apiModel.setTransformClosure(apiModelConfig.getTransformClosure());


                    ObjectNode objectNode = objectMapper.convertValue(resolvedApiModelAttributeMap, ObjectNode.class);

                    apiModel.setData(objectNode);
                    apiModel.transformModel();

                    if(apiModel.filterModel()){
                        JsonNode apiNode = objectMapper.convertValue(apiModel, JsonNode.class);
                        filteredResponse.add(apiNode);    
                    }
                }
            }

            else{

                Map resolvedApiModelAttributeMap = new HashMap<>();

                // Create a context 
                Map diggedResponseBodyAsMap = objectMapper.convertValue(diggedResponseBody, Map.class);
                
                actionResponse = this.actionContext.getActionResponse();
                actionResponse.setModel(diggedResponseBodyAsMap);            

                for(Map.Entry<String, Closure> fieldConfig: desiredFieldListConfig.entrySet()){
                    
                    Closure c = fieldConfig.getValue();
                    c.setDelegate(this.actionContext);
                    c.setResolveStrategy(Closure.DELEGATE_ONLY);
                    resolvedApiModelAttributeMap.put(fieldConfig.getKey(), c.call());
                }

                ApiModel apiModel = new ApiModel();
                apiModel.setName(apiModelConfig.getName());
                apiModel.setFilterClosure(apiModelConfig.getFilterClosure());
                apiModel.setTransformClosure(apiModelConfig.getTransformClosure());
                
                ObjectNode objectNode = objectMapper.convertValue(resolvedApiModelAttributeMap, ObjectNode.class);
                apiModel.setData(objectNode);
                apiModel.transformModel();

                if(apiModel.filterModel()){
                    JsonNode apiNode = objectMapper.convertValue(apiModel, JsonNode.class);
                    filteredResponse.add(apiNode);
                }
            }

            return filteredResponse;

        }

        catch(Exception e){

            e.printStackTrace();
            return null;
        }
    }

    /**
     *  HasMore hook will be called after one API call of the action. 
     * This will be called to know if this action is paginated action
     * @param requestResponseContainer
     * @param parentActionData
     * @return
     */
    public boolean hasMore(RequestResponseContainer requestResponseContainer, JsonNode... parentActionData){

        try{

            Hooks hook = this.actionResolver.resolveHook(actionConfig.getHooksConfig(),null);
            Closure<Boolean> hasMoreClosure = hook.getHasMoreClosure();

            if(hasMoreClosure == null){
                return false;
            }

            ActionRequest r = objectMapper.convertValue(requestResponseContainer.getRequest(), ActionRequest.class);
            ActionResponse actionResponse = objectMapper.convertValue(requestResponseContainer.getResponse(), ActionResponse.class); 
            
            this.actionContext.setActionRequest(r);
            this.actionContext.setActionResponse(actionResponse);

            return hook.call_hasMoreClosure(this.actionContext);
        }

        catch(Exception e){

            e.printStackTrace();
            return false;
        }
    }

    /**
     * After action is the hook that will be called once action is done. 
     * Any changes in the input, response by this closure will not have any effect 
     * @param requestResponseContainer
     * @param parentActionData
     */
    public void afterAction(RequestResponseContainer requestResponseContainer, JsonNode... parentActionData){

        try{

            Hooks hook = this.actionResolver.resolveHook(actionConfig.getHooksConfig(),null);
            Closure actionCloseClosure = hook.getActionCloseClosure();

            if(actionCloseClosure != null){
            
                ActionRequest r = objectMapper.convertValue(requestResponseContainer.getRequest(), ActionRequest.class);
                ActionResponse res = objectMapper.convertValue(requestResponseContainer.getResponse(), ActionResponse.class); 

                this.actionContext.setActionRequest(r);
                this.actionContext.setActionResponse(res);
                hook.call_actionCloseClosure(this.actionContext);
            }
        }

        catch(Exception e){
            e.printStackTrace();
        }

    }
    public JsonNode populateActionOutput(JsonNode jsonNode){

        List<ApiModel> apiModelList = actionServiceUtility.parseResponseToModel(jsonNode);
        ObjectNode finalResponse = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        for(ApiModel apiModel : apiModelList){

            OutputModelConfig outputModelConfig = null;
            
            if(this.actionContext.getActionSharedMap().containsKey("_custom_output_model")){
                String outputModelName = (String)this.actionContext.getActionSharedMap().get("_custom_output_model");
                outputModelConfig = this.modelSpec.getOutputModelByName(outputModelName);
            }
            else{

                // This needs to be changed, not just 0th , fill all output model 
                outputModelConfig = this.actionConfig.getOutputModelConfigList().get(0);
            }
            
            OutputModel outputModel = new OutputModel();
            outputModel.setName(outputModelConfig.getName());
            outputModel.setFilterClosure(outputModelConfig.getFilterClosure());
            outputModel.setTransformClosure(outputModelConfig.getTransformClosure());
            outputModel.setOutputFieldMapping(outputModelConfig.getOutputFieldMapping());

            if(outputModel.dependsOn(apiModel)){

                if(Boolean.TRUE.equals(outputModel.primitive())){
                    outputModel.populate(apiModel);
                    JsonNode outputModelNode = objectMapper.convertValue(outputModel, JsonNode.class);
                    arrayNode.add(outputModelNode);
                }

                else{

                    // it is partial 
                    // It means that it is partial 
                    Join join = outputModel.getJoin();   

                    if(join.getLeftModelKey().equalsIgnoreCase(apiModel.getName())){

                        //  Check if right filled exists already ? 
                        // if so then populate it with left model 
                    }

                    else{

                        // it is right model
                        // check if left filled already exists ? 
                        // if so then populate it with right filled 
                    }
                }
            }

        }

        finalResponse.set("response", arrayNode);

        if(this.actionContext.getActionSharedMap().containsKey("_response_type")){

            ACTION_HTTP_CODE httpCode = (ACTION_HTTP_CODE)this.actionContext.getActionSharedMap().get("_response_type");
            finalResponse.put("status", httpCode.toString());
        }
        else{
            finalResponse.put("status", ACTION_HTTP_CODE.OK.toString());
        }
        
        return finalResponse;
    }


    public Map<String, Object> createContext(Map<String, Object> context, ActionRequest request, JsonNode response){

        ObjectMapper objectMapper = new ObjectMapper();

        if(request != null){

            Map requestMap = objectMapper.convertValue(request, Map.class);
            context.put("_request", requestMap);
        }

        if(response != null){

            if(response.isArray()){

                List responseMap = objectMapper.convertValue(response, List.class);
                context.put("_response", responseMap);
            }

            else{

                Map responseMap = objectMapper.convertValue(response, Map.class);
                context.put("_response", responseMap);
            }
            
        }

        return context;
    }
}
