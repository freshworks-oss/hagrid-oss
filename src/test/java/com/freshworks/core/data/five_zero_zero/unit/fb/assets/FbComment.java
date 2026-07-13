package com.freshworks.core.data.five_zero_zero.unit.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshIndex;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Getter
@Setter
@Component("unit_fb_asset_fbComment")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Conditional(CustomRegExConditionComparator.class)
public class FbComment extends AbstractAsset {

    @FreshIndex
    String comment_id;

    @FreshIndex
    String comment_title;
    String comment_text;

    public void setBatchFromBean(com.freshworks.core.data.five_zero_zero.unit.fb.beans.FbComment comment){

        comment_id = comment.getComment_id();
        comment_title = comment.getComment_title();
        comment_text = comment.getComment_text();
    }

    @Override
    public void transform() {
//        System.out.println("Creating comment asset");
    }
}
