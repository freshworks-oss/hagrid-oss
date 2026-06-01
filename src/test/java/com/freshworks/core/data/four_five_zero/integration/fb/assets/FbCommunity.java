package com.freshworks.core.data.four_five_zero.integration.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.CustomRegExConditionComparator;
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
public class FbCommunity extends AbstractAsset {

    String community_id;
    String community_title;
    String community_description;

    public void setBatchFromBean(com.freshworks.core.data.four_zero_zero.integration.fb.beans.FbCommunity community){

        community_id = community.getCommunity_id();
        community_title = community.getCommunity_title();
        community_description = community.getCommunity_description();
    }

    @Override
    public void transform() {
    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
