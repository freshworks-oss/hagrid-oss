package com.freshworks.core.data.four_five_zero.unit.dag.assets.complex_asset.inner;

import com.freshworks.core.data.four_five_zero.unit.dag.assets.Usage;
import com.freshworks.core.data.four_five_zero.unit.dag.assets.complex_asset.Outer;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;

@FreshJoin(leftClass = Outer.class, leftClassFieldName = "id", 
    rightClass = Usage.class, rightClassFieldName = "usage", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "inner_app_usage_join" )
    
public class Inner extends AbstractAsset{

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
    
}
