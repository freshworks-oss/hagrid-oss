package com.freshworks.core.traverser.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TreeNodeTraversalException extends Exception{


    public TreeNodeTraversalException(String nodeName){
//        this.treeNode = Utility.getDag().find(nodeName);
    }
}
