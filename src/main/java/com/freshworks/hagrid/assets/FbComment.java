package com.freshworks.hagrid.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.annotation.Conditional;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FbComment extends AbstractAsset {

    String userId;
    String comment_id;
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.hagrid.beans.FbComment comment){

        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {

        // Simulating a case to perform join on userId to create non primitive asset FbUserComment.java
        this.userId = String.valueOf(ThreadLocalRandom.current().nextInt(0, 100));
    }

}
