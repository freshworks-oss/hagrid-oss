package com.freshworks.core.data.four_five_zero.unit.processor.joins.beans;

import com.freshworks.core.processor.AbstractBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class FbUsageBean extends AbstractBean{

    String userId;
    String createdAt;


    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
        
}
