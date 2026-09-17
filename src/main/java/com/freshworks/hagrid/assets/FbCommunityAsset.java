package com.freshworks.hagrid.assets;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FbCommunity is primitive asset as it is created from FbCommunity bean
 */

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Component 
@Scope ("prototype")
public class FbCommunityAsset extends AbstractAsset {

    String user_id;
    String community_id;
    String community_title;
    String community_description;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbCommunityBean community){

        user_id = community.getUser_id();
        community_id = community.getCommunity_id();
        community_title = community.getCommunity_title();
        community_description = community.getCommunity_description();
    }

    @Override
    public void transform() {
    }

}
