package com.freshworks.core.data.four_zero_zero.integration.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Conditional(CustomRegExConditionComparator.class)
public class FbComment extends AbstractBean {

    String comment_id;
    String comment_text;
    String comment_title;

    @Override
    public void transform() {

    }
}
