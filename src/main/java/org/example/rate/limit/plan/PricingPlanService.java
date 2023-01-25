package org.example.rate.limit.plan;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingPlanService {
  private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
  private final LettuceBasedProxyManager proxyManager;

  public Bucket resolveBucket(String apiKey) {
    return cache.computeIfAbsent(apiKey, this::newBucket);
  }

  private Bucket newBucket(String apiKey) {
    PricingPlan pricingPlan = PricingPlan.resolvePlanFromApiKey(apiKey);
    BucketConfiguration configuration =
        BucketConfiguration.builder().addLimit(pricingPlan.getLimit()).build();
    Bucket bucket = proxyManager.builder().build(apiKey, configuration);
    return Bucket.builder().addLimit(pricingPlan.getLimit()).build();
  }
}
