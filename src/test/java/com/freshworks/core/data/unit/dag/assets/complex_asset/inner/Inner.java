package com.freshworks.core.data.unit.dag.assets.complex_asset.inner;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.core.data.unit.dag.assets.Usage;
import com.freshworks.core.data.unit.dag.assets.complex_asset.Outer;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;

@FreshJoin(leftClass = Outer.class, leftClassFieldName = "id", 
    rightClass = Usage.class, rightClassFieldName = "usage", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "inner_app_usage_join" )
@Profile("unit")    
@Component("unit_dag_asset_Inner")
public class Inner extends AbstractAsset{

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }


    public void setData(Outer outer, Usage usage){
        
    }
    
}
