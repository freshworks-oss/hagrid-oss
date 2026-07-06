package com.freshworks.core.data.four_five_zero.unit.dag.assets.complex_asset.inner.inner_most;

import com.freshworks.core.data.four_five_zero.unit.dag.assets.Usage;
import com.freshworks.core.data.four_five_zero.unit.dag.assets.complex_asset.inner.Inner;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;

import lombok.Getter;
import lombok.Setter;

@FreshJoin(leftClass = Inner.class, leftClassFieldName = "id", 
    rightClass = Usage.class, rightClassFieldName = "usage", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "inner_app_usage_join" )
    

public class InnerMost extends AbstractAsset{

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
    
    public void setData(Inner inner, Usage usage){
        
    }
}
