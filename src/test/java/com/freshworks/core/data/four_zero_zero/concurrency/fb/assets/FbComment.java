package com.freshworks.core.data.four_zero_zero.concurrency.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Conditional(CustomRegExConditionComparator.class)
public class FbComment extends AbstractAsset {

    String comment_id;
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.core.data.four_zero_zero.concurrency.fb.beans.FbComment comment){

        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {
//        System.out.println("Creating comment asset");
    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
