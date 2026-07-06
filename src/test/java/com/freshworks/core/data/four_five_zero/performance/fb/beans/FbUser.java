package com.freshworks.core.data.four_five_zero.performance.fb.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.SyncServiceContainer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.annotation.Conditional;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Conditional(CustomRegExConditionComparator.class)
public class FbUser extends AbstractBean {

    String user_id;
    String user_name;
    String extra_bytes;


    @Override
    public void transform() {

        int randomId = 1000;
        this.user_id = String.valueOf(randomId);

        // int sizeInBytes = 8 * 1024 * 1024; // 8388608 bytes
        
        // StringBuilder sb = new StringBuilder(sizeInBytes);
        // for (int i = 0; i < sizeInBytes; i++) {
        //     sb.append('a'); 
        // }
        
        // this.extra_bytes = sb.toString();

    }
}
