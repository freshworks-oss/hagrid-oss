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

    String userId;
    String userName;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbUser dummy){

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {

        // Simulating a case to perform join on userId to create non primitive asset FbUserComment.java
        this.userId = String.valueOf(ThreadLocalRandom.current().nextInt(0, 100));
    }
}
