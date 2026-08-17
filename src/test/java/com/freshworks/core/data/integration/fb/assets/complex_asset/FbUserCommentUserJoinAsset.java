package com.freshworks.core.data.integration.fb.assets.complex_asset;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.core.data.integration.fb.assets.FbComment;
import com.freshworks.core.data.integration.fb.assets.FbUser;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;

@Profile("integration")
@FreshJoin(leftClass = FbUserComment.class, leftClassFieldName = "userId",
        rightClass = FbUser.class, rightClassFieldName = "userId", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "fbusercomment_fbuser_inner_join")
@Component("recursive_contextual_asset_fbUserCommentUser")
public class FbUserCommentUserJoinAsset extends AbstractAsset {

    String userId;
    String commentId;
    String commentText;

    public void setFromAsset(FbUserComment fbUserComment, FbUser fbUser){
        this.userId = fbUser.getUserId();
        this.commentId = fbUserComment.getCommentId();
        this.commentText = fbUserComment.getCommentText();
    }


    @Override
    public void transform() {


    }
}
