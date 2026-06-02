package com.freshworks.core.data.four_five_zero.integration.fb.assets.complex_asset;

import com.freshworks.core.data.four_five_zero.integration.fb.assets.FbComment;
import com.freshworks.core.data.four_five_zero.integration.fb.assets.FbUser;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;


@FreshJoin(leftClass = FbUser.class, leftClassFieldName = "userId", 
        rightClass = FbComment.class, rightClassFieldName = "userId", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "fbuser_fbcomment_inner_join")
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
    
}
