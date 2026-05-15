package com.freshworks.core.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.constants.Constants;
import com.google.common.base.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = Constants.JsonTypeInfo_As_PROPERTY, visible = true)
public abstract class AbstractAsset {

    String uniqueIdentifier = null;

    public Optional<Boolean> filter(){
        return Optional.fromNullable(true);
    };

    @JsonIgnore
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    public abstract void transform();

    public abstract Object getUniqueIdentifier();

    public Class<? extends AbstractBean> publishAsBean(){
        return null;
    }

    public void setUniqueIdentifier(String uniqueIdentifier){
        this.uniqueIdentifier = uniqueIdentifier;
    }
}
