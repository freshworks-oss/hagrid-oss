package com.freshworks.core.data.integration.fb.assets.complex_asset;

import org.springframework.context.annotation.Profile;

import com.freshworks.core.data.integration.fb.assets.FbComment;
import com.freshworks.core.data.integration.fb.assets.FbUser;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@FreshJoin(leftClass = FbUser.class, leftClassFieldName = "userId", 
        rightClass = FbComment.class, rightClassFieldName = "userId", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "fbuser_fbcomment_inner_join")
@Profile("integration")
public class FbUserComment extends AbstractAsset{

    String userId;
    String commentId;
    String commentText;

    public void setFromAsset(FbUser fbUser, FbComment fbComment){
        this.userId = fbUser.getUserId();
        this.commentId = fbComment.getComment_id();
        this.commentText = fbComment.getComment_text();
    }


    @Override
    public void transform() {
    
    
    }
    
}
