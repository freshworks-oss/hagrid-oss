package com.freshworks.core.data.concurrency.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.hagrid.processor.AbstractBean;

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
@Profile("concurrency")
@Component 
public class FbCommunityBean extends AbstractBean {

    String community_id;
    String community_title;
    String community_description;

    @Override
    public void transform() {

    }
}
