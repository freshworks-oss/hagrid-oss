package com.freshworks.core.data.unit.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.processor.AbstractBean;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component("unit_fb_bean_fbComment")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("unit")
public class FbComment extends AbstractBean {

    String comment_id;
    String comment_text;
    String comment_title;

    @Override
    public void transform() {

    }
}
