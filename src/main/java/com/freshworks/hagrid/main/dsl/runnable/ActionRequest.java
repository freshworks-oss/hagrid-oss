package com.freshworks.hagrid.main.dsl.runnable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.freshworks.hagrid.shared.constants.Constants;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = Constants.JsonTypeInfo_As_PROPERTY, visible = true)
public class ActionRequest {
    
}
