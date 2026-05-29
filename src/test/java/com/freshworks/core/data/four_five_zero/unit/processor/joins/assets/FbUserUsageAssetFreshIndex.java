package com.freshworks.core.data.four_zero_zero.unit.processor.joins.assets;

import com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUsageBean;
import com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUserBean;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshIndex;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.Annotations.FreshJoin.JOIN_TYPE;

import lombok.Getter;
import lombok.Setter;




@Getter
@Setter
public class FbUserUsageAssetFreshIndex extends AbstractAsset{
    
    @FreshIndex
    String userId;

    String firstName;
    String lastName;
    String createdAt;

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
