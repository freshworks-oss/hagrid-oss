package com.freshworks.core.data.unit.fb.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Getter
@Setter
@Component("unit_fb_asset_fbUser")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("unit")
public class FbUser extends AbstractAsset {

    String userId;
    String userName;

    public void setBatchFromBean(com.freshworks.core.data.unit.fb.beans.FbUser dummy){

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {
//        System.out.println("Creating user asset");
    }
}
