package com.freshworks.hagrid.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshAsset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * FbComment is primitive asset as it is created from FbComment bean
 */

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Component 
@Scope ("prototype")
public class FbCommentAsset extends AbstractAsset {

    String user_id;
    String post_id;
    String comment_id;
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbCommentBean comment){

        user_id = comment.getUser_id();
        post_id = comment.getPost_id();
        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {
    }

}
