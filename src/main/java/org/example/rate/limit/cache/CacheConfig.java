package org.example.rate.limit.cache;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {
  private final CacheProperties cacheProperties;

  @Bean
  @Profile("local")
  public LettuceConnectionFactory localLettuceConnectionFactory() {
    if (log.isInfoEnabled()) {
      log.info(
          "Connecting to local redis server at {}:{}, isAuthEnabled: {}, defaultTimeToLive: {}, "
              + "defaultValueSerializer:{}",
          cacheProperties.getHost(),
          cacheProperties.getPort(),
          cacheProperties.isAuthEnabled(),
          cacheProperties.getDefaultTimeToLive(),
          cacheProperties.getDefaultValueSerializer());
    }
    RedisStandaloneConfiguration standaloneConfiguration =
        new RedisStandaloneConfiguration(cacheProperties.getHost(), cacheProperties.getPort());
    if (cacheProperties.isAuthEnabled()) {
      if (log.isInfoEnabled()) {
        log.info("userName: {}", cacheProperties.getUsername());
      }
      standaloneConfiguration.setUsername(cacheProperties.getUsername());
      standaloneConfiguration.setPassword(cacheProperties.getPassword());
    }
    return new LettuceConnectionFactory(standaloneConfiguration);
  }

  @Bean
  @Profile("!local")
  public LettuceConnectionFactory lettuceConnectionFactory() {
    if (log.isInfoEnabled()) {
      log.info(
          "Connecting to AWS redis cluster at {}:{}, isAuthEnabled: {}, defaultTimeToLive: {}"
              + "defaultValueSerializer:{}",
          cacheProperties.getHost(),
          cacheProperties.getPort(),
          cacheProperties.isAuthEnabled(),
          cacheProperties.getDefaultTimeToLive(),
          cacheProperties.getDefaultValueSerializer());
    }
    final List<String> nodes =
        Collections.singletonList(cacheProperties.getHost() + ":" + cacheProperties.getPort());
    RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(nodes);

    if (cacheProperties.isAuthEnabled()) {
      if (log.isInfoEnabled()) {
        log.info("userName: {}", cacheProperties.getUsername());
      }
      clusterConfiguration.setUsername(cacheProperties.getUsername());
      clusterConfiguration.setPassword(RedisPassword.of(cacheProperties.getPassword()));
    }

    ClusterTopologyRefreshOptions refreshOptions =
        ClusterTopologyRefreshOptions.builder()
            .closeStaleConnections(true)
            .enableAllAdaptiveRefreshTriggers()
            .build();

    ClusterClientOptions clientOptions =
        ClusterClientOptions.builder()
            .autoReconnect(true)
            .topologyRefreshOptions(refreshOptions)
            .validateClusterNodeMembership(false)
            .build();

    LettuceClientConfiguration clientConfiguration =
        LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .useSsl()
            .build();

    return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return builder -> customize(builder, cacheProperties);
  }

  private void customize(RedisCacheManagerBuilder builder, CacheProperties cacheProperties) {
    final RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .entryTtl(cacheProperties.getDefaultTimeToLive());
    builder.cacheDefaults(config);
    if (log.isInfoEnabled()) {
      log.info("cacheConfig: {}", cacheProperties.getConfigByName());
    }
    cacheProperties
        .getConfigByName()
        .forEach(
            (name, options) ->
                builder.withCacheConfiguration(
                    name,
                    config
                        .entryTtl(
                            options.getTimeToLive() != null
                                ? options.getTimeToLive()
                                : cacheProperties.getDefaultTimeToLive())
                        .serializeValuesWith(
                            getValueSerializer(
                                options.getValueSerializer() != null
                                    ? options.getValueSerializer()
                                    : cacheProperties.getDefaultValueSerializer()))));
  }

  private SerializationPair<Object> getValueSerializer(CacheValueSerializer cacheValueSerializer) {
    if (cacheValueSerializer == CacheValueSerializer.JDK_SERIALIZER) {
      return SerializationPair.fromSerializer(new JdkSerializationRedisSerializer());
    }
    return SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());
  }
}
