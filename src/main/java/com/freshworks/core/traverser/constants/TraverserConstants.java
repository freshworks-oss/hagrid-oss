package com.freshworks.core.traverser.constants;

public class TraverserConstants {

    public static String TRAVERSER_PATH = "/configuration/core/traverser";;

    public static String TRAVERSER_THREAD_COUNT = "/configuration/core/traverser/thread_count";;
    public static String TRAVERSER_TYPE = "/configuration/core/traverser/type";

    public enum TRAVERSE_STATUS{
        TRAVERSE_SUCCESS,
        TRAVERSE_FAILED,
        TRAVERSE_ON_HOLD,
        TRAVERSE_ABORT,
        TRAVERSE_IN_PROGRESS,
    }
}
