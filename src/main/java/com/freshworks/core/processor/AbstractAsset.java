package com.freshworks.core.processor;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.constants.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = Constants.JsonTypeInfo_As_PROPERTY, visible = true)
public abstract class AbstractAsset {

    String uuid = UUID.randomUUID().toString();
    Long created_at_ms = System.currentTimeMillis();

    @JsonIgnore
    SyncServiceContainer syncServiceContainer;

    public boolean filter(){
        return true;
    };


    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    public abstract void transform();


}
