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
 * FbPost is primitive asset as it is created from FbPost bean
 */

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Component 
@Scope ("prototype")
public class FbPostAsset extends AbstractAsset {

    String user_id;
    String post_id;
    String post_title;
    String post_text;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbPostBean post){
        
        user_id = post.getUser_id();
        post_id = post.getPost_id();
        post_title = post.getPost_title();
        post_text = post.getPost_text();
    }

    @Override
    public void transform() {
    }
}
