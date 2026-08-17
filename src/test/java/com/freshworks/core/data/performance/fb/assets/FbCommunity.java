package com.freshworks.core.data.performance.fb.assets;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("performance")
@Component("fb_asset_fbCommunity")
public class FbCommunity extends AbstractAsset {

    String community_id;
    String community_title;
    String community_description;

    public void setBatchFromBean(com.freshworks.core.data.performance.fb.beans.FbCommunity community){

        community_id = community.getCommunity_id();
        community_title = community.getCommunity_title();
        community_description = community.getCommunity_description();
    }

    @Override
    public void transform() {
    }

}
