package com.freshworks.hagrid.main.dsl.bean;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.hagrid.main.dsl.config.model.ApiModelConfig;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;

@SpringBootTest 
public class TestActionSpecBean {
    
    @Test 
    public void testActionSpecBeanWhenActionDslContainsOnlySimpleAction(){

        ModelSpec modelSpec = new ModelSpec();
        
        ApiModelConfig apiModelConfig1 = new ApiModelConfig();
        
        modelSpec.getApiModelList().addLast(apiModelConfig1);
    }


}
