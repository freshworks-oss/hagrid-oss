package com.freshworks.core.data.performance.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.processor.AbstractBean;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("performance")
public class FbComment extends AbstractBean {

    String user_id;
    String comment_id;
    String comment_text;
    String comment_title;

    @Override
    public void transform() {

        int randomId = 1000;
        this.user_id = String.valueOf(randomId);
    }
}
