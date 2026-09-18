package com.freshworks.core.data.durability.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.hagrid.processor.AbstractAsset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("durability")
@Component 
public class FbCommunityAsset extends AbstractAsset {

    String community_id;
    String community_title;
    String community_description;

    public void setBatchFromBean(com.freshworks.core.data.durability.beans.FbCommunityBean community){

        community_id = community.getCommunity_id();
        community_title = community.getCommunity_title();
        community_description = community.getCommunity_description();
    }

    @Override
    public void transform() {
    }

}
