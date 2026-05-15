package com.freshworks.core.data.four_zero_zero.unit.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.SyncServiceContainer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Conditional(CustomRegExConditionComparator.class)
public class FbPost extends AbstractBean {

    String post_id;
    String post_title;
    String post_text;

    SyncServiceContainer syncServiceContainer;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    @Override
    public void transform() {

    }
}
