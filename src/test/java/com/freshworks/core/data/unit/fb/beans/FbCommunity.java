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
@Component("unit_fb_bean_fbCommunity")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("unit")
public class FbCommunity extends AbstractBean {

    String community_id;
    String community_title;
    String community_description;

    @Override
    public void transform() {

    }
}
