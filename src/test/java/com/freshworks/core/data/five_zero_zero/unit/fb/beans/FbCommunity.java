package com.freshworks.core.data.five_zero_zero.unit.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component("unit_fb_bean_fbCommunity")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Conditional(CustomRegExConditionComparator.class)
public class FbCommunity extends AbstractBean {

    String community_id;
    String community_title;
    String community_description;

    @Override
    public void transform() {

    }
}
