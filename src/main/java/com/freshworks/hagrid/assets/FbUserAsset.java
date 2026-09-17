package com.freshworks.hagrid.assets;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * FbUser is primitive asset as it is created from FbUser bean
 */
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Component 
@Scope ("prototype")
public class FbUserAsset extends AbstractAsset {

    String user_id;
    String user_name;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbUserBean user){

        user_id = user.getUser_id();
        user_name = user.getUser_name();
    }


    @Override
    public void transform() {
    }
}
