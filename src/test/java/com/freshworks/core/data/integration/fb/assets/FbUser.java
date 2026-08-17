package com.freshworks.core.data.integration.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;

import io.netty.util.internal.ThreadLocalRandom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("integration")
@Component("recursive_contextual_asset_fbUser")
public class FbUser extends AbstractAsset {

    String userId;
    String userName;

    public void setBatchFromBean(com.freshworks.core.data.integration.fb.beans.FbUser dummy){

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {

        // I am setting this id to test non primitive assets by combining this with comment primitive asset 
        userId = "1000";
    }
}
