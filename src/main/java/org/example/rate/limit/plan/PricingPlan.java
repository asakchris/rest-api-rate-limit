package org.example.rate.limit.plan;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import java.time.Duration;

public enum PricingPlan {
  FREE(20),
  BASIC(40),
  PROFESSIONAL(100);

  private int bucketCapacity;

  private PricingPlan(int bucketCapacity) {
    this.bucketCapacity = bucketCapacity;
  }

  Bandwidth getLimit() {
    return Bandwidth.classic(
        bucketCapacity, Refill.intervally(bucketCapacity, Duration.ofHours(1)));
  }

  public int bucketCapacity() {
    return bucketCapacity;
  }

  static PricingPlan resolvePlanFromApiKey(String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) {
      return FREE;
    } else if (apiKey.startsWith("PX001-")) {
      return PROFESSIONAL;
    } else if (apiKey.startsWith("BX001-")) {
      return BASIC;
    }
    return FREE;
  }
}
