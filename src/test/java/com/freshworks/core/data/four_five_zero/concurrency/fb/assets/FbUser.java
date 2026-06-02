package com.freshworks.core.data.four_five_zero.concurrency.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Conditional(CustomRegExConditionComparator.class)
public class FbUser extends AbstractAsset {

    String userId;
    String userName;

    public void setBatchFromBean(com.freshworks.core.data.four_five_zero.concurrency.fb.beans.FbUser dummy){

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {
//        System.out.println("Creating user asset");
    }

}
