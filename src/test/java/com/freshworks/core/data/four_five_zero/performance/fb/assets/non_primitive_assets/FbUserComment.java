package com.freshworks.core.data.four_five_zero.performance.fb.assets.non_primitive_assets;

import com.freshworks.core.data.four_five_zero.performance.fb.assets.FbUser;
import com.freshworks.core.data.four_five_zero.performance.fb.assets.FbComment;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FreshJoin(leftClass = FbUser.class, leftClassFieldName = "userId", 
        rightClass = FbComment.class, rightClassFieldName = "userId", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "fbuser_fbcomment_inner_join")
@FreshAsset(ignore = true)
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
