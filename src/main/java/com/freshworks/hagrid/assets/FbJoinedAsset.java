package com.freshworks.hagrid.assets;

import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;

import lombok.Getter;
import lombok.Setter;


/**
 * This is some hypothetical non primitive asset which is suppose to be created by joining FbComment and FbUser ..
 * Checkout the definiton of FreshJoin below 
 */
@FreshJoin(rightClass = FbComment.class, uniqueJoinName = "user_comment_inner_join", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN,rightClassFieldName = "userId", leftClass = FbUser.class , leftClassFieldName = "userId")
@FreshAsset(ignore = false)
@Getter
@Setter
public class FbJoinedAsset extends AbstractAsset{
    

    String userId;
    String userName;
    String commentText;

    public void setData(FbUser fbUser, FbComment fbComment){
        this.userId = fbUser.getUserId();
        this.commentText = fbComment.getComment_text();
    }

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }

}
