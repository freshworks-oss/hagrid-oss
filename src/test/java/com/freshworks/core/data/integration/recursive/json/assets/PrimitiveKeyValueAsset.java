package com.freshworks.core.data.integration.recursive.json.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.data.integration.recursive.json.beans.PrimitiveKeyValue;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("integration")
public class PrimitiveKeyValueAsset extends AbstractAsset {

    String key;
    String value;

    public void setBatchFromBean(PrimitiveKeyValue dummy){

        this.key = dummy.getKey();
        this.value = dummy.getValue();
    }


    @Override
    public void transform() {
    }
}
