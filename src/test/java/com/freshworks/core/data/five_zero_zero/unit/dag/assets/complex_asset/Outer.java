package com.freshworks.core.data.five_zero_zero.unit.dag.assets.complex_asset;

import org.springframework.stereotype.Component;

import com.freshworks.core.data.five_zero_zero.unit.dag.assets.Application;
import com.freshworks.core.data.five_zero_zero.unit.dag.assets.Usage;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;


@FreshJoin(leftClass = Application.class, leftClassFieldName = "id", 
    rightClass = Usage.class, rightClassFieldName = "usage", join_type = FreshJoin.JOIN_TYPE.INNER_JOIN, uniqueJoinName = "inner_app_usage_join" )

@Component("unit_dag_asset_Outer")
public class Outer extends AbstractAsset{

    String id;

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }

    public void setFromAsset(Application application, Usage usage){

    }
}
