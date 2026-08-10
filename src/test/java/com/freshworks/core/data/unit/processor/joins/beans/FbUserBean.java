package com.freshworks.core.data.unit.processor.joins.beans;

import org.springframework.context.annotation.Profile;

import com.freshworks.core.processor.AbstractBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Profile("unit")
public class FbUserBean extends AbstractBean{
    
    String id;
    String firstName;
    String lastName;
    
    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }

}
