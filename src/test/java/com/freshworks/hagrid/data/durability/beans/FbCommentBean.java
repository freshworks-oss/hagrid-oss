package com.freshworks.core.data.durability.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.hagrid.processor.AbstractBean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("durability")
@Component 
public class FbCommentBean extends AbstractBean {

    String comment_id;
    String comment_text;
    String comment_title;

    @Override
    public void transform() {

    }
}
