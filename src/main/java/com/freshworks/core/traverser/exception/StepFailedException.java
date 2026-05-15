package com.freshworks.core.traverser.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.traverser.AbstractStep;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StepFailedException extends Exception{

    JsonNode jsonNode;
    Class<? extends  AbstractBean> abstractBean;

    AbstractStep abstractStep;

    public StepFailedException(AbstractStep step, JsonNode jsonNode, Class<? extends AbstractBean> beanClazz){
        this.abstractBean = beanClazz;
        this.jsonNode = jsonNode;
        this.abstractStep = step;
    }

    public StepFailedException(JsonNode jsonNode, Class<? extends AbstractBean> beanClazz){
        this.abstractBean = beanClazz;
        this.jsonNode = jsonNode;
    }
}
