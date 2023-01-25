package org.example.rate.limit.cache;

public enum CacheValueSerializer {
  JDK_SERIALIZER("JDK_SERIALIZER", "Java Binary Serializer"),
  GENERIC_JSON_SERIALIZER("GENERIC_JSON_SERIALIZER", "Generic JSON Serializer");

  private final String code;
  private final String description;

  CacheValueSerializer(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }
}
