package com.freshworks.core.data.four_zero_zero.integration.recursive.contextual.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_zero_zero.integration.recursive.contextual.beans.PublishedBean;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Conditional(CustomRegExConditionComparator.class)
public class PublishedAsset extends AbstractAsset {

    String token;
    String context;

    public void setBatchFromBean(PublishedBean dummy){

        this.token = dummy.getToken();
        this.context = dummy.getContext();
    }


    @Override
    public void transform() {
    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
