package com.freshworks.hagrid.assets;

import java.util.concurrent.ThreadLocalRandom;

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
public class FbUser extends AbstractAsset {

    String user_id;
    String user_name;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbUser user){

        user_id = user.getUser_id();
        user_name = user.getUser_name();
    }


    @Override
    public void transform() {
    }
}
