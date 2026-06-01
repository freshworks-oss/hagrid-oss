package com.freshworks.core.data.four_five_zero.unit.processor.joins.assets;

import com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUsageBean;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.Annotations.FreshJoin.JOIN_TYPE;



@FreshJoin(rightClass = FbUsageBean.class, uniqueJoinName = "usage_user_left_join", join_type = JOIN_TYPE.LEFT_JOIN,
    onFieldList = {
        @FreshJoin.OnField(rightClassFieldName = "userId", leftClass = FbUserBean.class , leftClassFieldName = "id")
    }
)

public class FbUserUsageAsset extends AbstractAsset{
    

    String userId;
    String firstName;
    String lastName;
    String createdAt;

    public void setFromUsageBean(FbUsageBean fbUsageBean){
        this.userId = fbUsageBean.getUserId();
        this.createdAt = fbUsageBean.getCreatedAt();
    }

    public void setFromUserBean(FbUserBean fbUserBean){
        this.firstName = fbUserBean.getFirstName();
        this.lastName = fbUserBean.getLastName();
    }

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }

    @Override
    public Object getUniqueIdentifier() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUniqueIdentifier'");
    }


}
