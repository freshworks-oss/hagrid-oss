package com.freshworks.hagrid.assets;

import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.Annotations.FreshJoin.JOIN_TYPE;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@FreshJoin(leftClass = FbComment.class, rightClass = FbCommunity.class, leftClassFieldName = "user_id", rightClassFieldName = "user_id", 
    join_type = JOIN_TYPE.INNER_JOIN, uniqueJoinName = "comment_community_inner_join")

public class FbNonPrimitiveAsset extends AbstractAsset {


    String user_id;
    String community_id;
    String comment_id;


    public void set(FbComment fbComment, FbCommunity fbCommunity){
        this.user_id = fbComment.getUser_id();
        this.comment_id = fbComment.getComment_id();
        this.community_id = fbCommunity.getCommunity_id();
    }

    @Override
    public void transform() {
        
        System.out.println("generated with " + this.user_id + " commentId " + this.comment_id + " community_id " + this.community_id);
    }
    
}
