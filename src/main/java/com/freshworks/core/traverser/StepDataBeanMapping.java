package com.freshworks.core.traverser;


import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.processor.AbstractBean;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepDataBeanMapping {

    JsonNode parseSyncedResponseData;
    Class<? extends AbstractBean> beanClass;
    boolean passToChildNodes = true;
}
