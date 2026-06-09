package com.freshworks.core.data.four_five_zero.integration.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;

import io.netty.util.internal.ThreadLocalRandom;
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

    String userId;
    String comment_id;
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.core.data.four_five_zero.integration.fb.beans.FbComment comment){

        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {

        // int randomNumber = ThreadLocalRandom.current().nextInt(0, 10);
        userId = "1000";
    }
}
