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
@Component("unit_fb_asset_fbPost")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Profile("unit")
public class FbPost extends AbstractAsset {

    String post_id;
    String post_title;
    String post_text;

    public void setBatchFromBean(com.freshworks.core.data.unit.fb.beans.FbPost post){

        post_id = post.getPost_id();
        post_title = post.getPost_title();
        post_text = post.getPost_text();
    }

    @Override
    public void transform() {
//        System.out.println("Creating post asset");
    }

}
