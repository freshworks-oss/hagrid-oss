package com.freshworks.core.data.concurrency.fb.assets;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.hagrid.processor.AbstractAsset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("concurrency")
@Component 
public class FbCommentAsset extends AbstractAsset {

    String comment_id;
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.core.data.concurrency.fb.beans.FbCommentBean comment){

        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {
//        System.out.println("Creating comment asset");
    }

}
