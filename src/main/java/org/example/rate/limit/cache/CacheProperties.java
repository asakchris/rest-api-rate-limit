package org.example.rate.limit.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cache")
@Data
public class CacheProperties {
  private String host;
  private int port;
  private boolean authEnabled;
  private String username;
  private String password;
  private Duration defaultTimeToLive = Duration.ofMinutes(30L);
  private CacheValueSerializer defaultValueSerializer =
      CacheValueSerializer.GENERIC_JSON_SERIALIZER;
  private Map<String, CacheOptions> configByName = new HashMap<>();
}

@Data
class CacheOptions {
  private Duration timeToLive;
  private CacheValueSerializer valueSerializer;
}
