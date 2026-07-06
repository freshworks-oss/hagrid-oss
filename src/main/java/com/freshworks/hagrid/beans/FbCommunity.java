package com.freshworks.hagrid.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.processor.AbstractBean;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FbCommunity extends AbstractBean {

    String user_id;
    String community_id;
    String community_title;
    String community_description;

    @Override
    public void transform() {

    }
}
