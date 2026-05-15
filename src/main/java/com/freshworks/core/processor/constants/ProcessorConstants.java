package com.freshworks.core.processor.constants;

public class ProcessorConstants {


    public static String PROCESSOR_PATH = "/configuration/core/processor";;

    public static String PROCESSOR_THREAD_COUNT = "/configuration/core/processor/thread_count";;

    public static String PROCESSOR_POLL_COUNT = "/configuration/core/processor/poll_count";;

    public static String NUMBER_OF_PARALLEL_PROCESSOR = "/configuration/core/processor/number_of_parallel_processor";


    public enum PROCESS_STATUS{
        PROCESS_SUCCESS,
        PROCESS_FAILED,
        PROCESS_ON_HOLD,
        PROCESS_ABORT,

        PROCESS_IN_PROGRESS
    }
}
