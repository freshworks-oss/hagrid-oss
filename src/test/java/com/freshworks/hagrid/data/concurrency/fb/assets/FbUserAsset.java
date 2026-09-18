package com.freshworks.core.data.concurrency.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.hagrid.processor.AbstractAsset;

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
@Profile("concurrency")
@Component 
public class FbUserAsset extends AbstractAsset {

    String userId;
    String userName;

    public void setBatchFromBean(com.freshworks.core.data.concurrency.fb.beans.FbUserBean dummy){

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {
//        System.out.println("Creating user asset");
    }

}
