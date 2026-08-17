package com.freshworks.core.data.integration.fb.beans;

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
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("integration")
@Component("recursive_contextual_bean_fbComment")
public class FbComment extends AbstractBean {

    String user_id;
    String comment_id;
    String comment_text;
    String comment_title;

    @Override
    public void transform() {

        this.user_id = "1000";
    }
}
