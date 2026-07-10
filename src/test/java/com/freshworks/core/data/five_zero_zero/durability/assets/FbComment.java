package com.freshworks.core.data.five_zero_zero.durability.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("durability & 3.7.0")
public class FbComment extends AbstractAsset {

    String comment_id;
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.core.data.five_zero_zero.durability.beans.FbComment comment){

        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {
//        System.out.println("Creating comment asset");
    }

}
