package com.freshworks.core.data.four_five_zero.unit.processor.joins.beans;

import com.freshworks.core.processor.AbstractBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class FbApplicationBean extends AbstractBean{

    String applicationId;
    String applicationName;

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
    
}
