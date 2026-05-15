package com.freshworks.core.shared.constants;

import com.google.common.collect.Lists;

import java.util.List;

public class Constants {

    public final static String JsonTypeInfo_As_PROPERTY = "clazz";
    public final static String GETTER_METHOD_PREFIX = "get";

    public static String HAGRID_CONFIG_ENV_NAME = "hagrid_config";
    public final static String SYNC_STATUS_KEY = "sync_status";

    public final static List<String> SUPPORTED_TRAVERSER_TYPE = Lists.newArrayList("http", "grpc");
    public final static String HTTP_TRAVERSER_TYPE = "http";
    public final static String GRPC_TRAVERSER_TYPE = "grpc";
    public final static String BASE_BEAN = "baseBean";

    public enum SYNC_STATUS{
        START,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        ABORT,
        ON_HOLD
    }
}
