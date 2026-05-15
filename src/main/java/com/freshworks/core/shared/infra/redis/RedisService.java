package com.freshworks.core.shared.infra.redis;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;

@NoArgsConstructor
public class RedisService implements InfraService {

    SyncServiceContainer syncServiceContainer;

    static HashMap<String, RedisConnection> singleton = new HashMap<>();

    RedisConnection redisConnection;

    InfraConfigService infraConfigService;

    int dbId;


    @Override
    public void configure(SyncServiceContainer syncServiceContainer,  InfraConfigService infraConfigService) throws IOException {
        this.syncServiceContainer = syncServiceContainer;
        String namespace = syncServiceContainer.getBean(Namespace.class).getNamespace();
        this.infraConfigService = infraConfigService;

        if(singleton.containsKey(namespace)){
            this.redisConnection = singleton.get(namespace);
        }
        else{
            this.dbId = Integer.parseInt(namespace);
            String dbUserName = infraConfigService.getDatabaseUserName();
            String dbPassword = infraConfigService.getDatabasePassword();
            String dbHost = infraConfigService.getDatabaseHost();
            Integer dbPort = infraConfigService.getDatabasePort();

            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setHostName(dbHost);
            redisStandaloneConfiguration.setPort(dbPort);
            redisStandaloneConfiguration.setDatabase(dbId);
            redisStandaloneConfiguration.setPassword(RedisPassword.of(dbPassword));
            redisStandaloneConfiguration.setUsername(dbUserName);

            JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfiguration = JedisClientConfiguration.builder();
            jedisClientConfiguration.connectTimeout(Duration.ofSeconds(60));// 60s connection timeout

            JedisConnectionFactory jedisConFactory = new JedisConnectionFactory(redisStandaloneConfiguration,
                    jedisClientConfiguration.build());

            this.redisConnection = jedisConFactory.getConnection();
            singleton.put(namespace, this.redisConnection);
        }
    }

    @Override
    public RedisQueue getProcessorQueue() throws Exception{

        return RedisQueue.getRedisQueue(this.redisConnection, "processor");
    }

    @Override
    public RedisKeyValue getKeyValue() throws Exception{

        return RedisKeyValue.getRedisKeyValue(this.redisConnection, "key_value");
    }

    @Override
    public InfraDbList getPublisherList() throws Exception {
        return null;
    }

    @Override
    public RedisList getInfraDbList(String listName) throws Exception{

        return RedisList.getRedisList(this.redisConnection, listName);
    }


    @Override
    public JsonQueryService getJsonQueryService() throws Exception{

        JsonQueryService jsonQueryService = syncServiceContainer.getBean(JsonQueryService.class);
        jsonQueryService.configure(String.valueOf(this.dbId));
        return  jsonQueryService;
    }

    @Override
    public NamespaceService getNamespaceService() throws Exception{

        NamespaceService namespaceService = syncServiceContainer.getBean(NamespaceService.class);
        return namespaceService;
    }


    @Override
    public void destroyFreshIndex() throws Exception{

        getNamespaceService().clearnNamespace(String.valueOf(this.dbId));
    }

    @Override
    public JsonIndexService getJsonIndexService() throws Exception{

        JsonIndexService jsonIndexService = syncServiceContainer.getBean(JsonIndexService.class);
        jsonIndexService.configure(String.valueOf(this.dbId));
        return  jsonIndexService;
    }

    @Override
    public String getNamespace() throws Exception{
        return Integer.toString(this.dbId);
    }

    public void destroy() throws Exception{

        // TODO: here destroy the redis infra for this namespace

        // We need to clear the freshIndex as well.
        destroyFreshIndex();
    }

}
