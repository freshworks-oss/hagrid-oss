package com.freshworks.core.data.five_zero_zero.unit.processor.joins.assets.non_primitive_assets;

import org.springframework.stereotype.Component;

import com.freshworks.core.data.five_zero_zero.unit.processor.joins.assets.FbUsageAsset;
import com.freshworks.core.data.five_zero_zero.unit.processor.joins.assets.FbUserAsset;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.Annotations.FreshJoin.JOIN_TYPE;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@FreshJoin(rightClass = FbUsageAsset.class, uniqueJoinName = "usage_user_inner_join", join_type = JOIN_TYPE.INNER_JOIN,rightClassFieldName = "userId", leftClass = FbUserAsset.class , leftClassFieldName = "userId")
@Component
@NoArgsConstructor
@Getter
@Setter
public class FbUserUsageAssetInnerJoin extends AbstractAsset{
    

    String userId;
    String userName;
    String createdAt;

    public void setFromUsageBean(FbUsageAsset fbUsageAsset){
        this.userId = fbUsageAsset.getUserId();
        this.createdAt = fbUsageAsset.getCreatedAt();
    }

    public void setFromUserBean(FbUserAsset fbUserAsset){
        this.userId = fbUserAsset.getUserId();
        this.userName = fbUserAsset.getUserName();
    }

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }

}
