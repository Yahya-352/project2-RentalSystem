package com.ga.RentalSystem.service;

import java.util.HashMap;
import java.util.Map;

public class RateLimitService {
    private final Map<String , TokenBucket> buckets = new HashMap<>();

    public Integer checkLimit(String key , int capacity , int window){
        double refillRate = capacity / window;

        buckets.putIfAbsent(key,new TokenBucket(capacity , refillRate));
        TokenBucket bucket = buckets.get(key);

        if(bucket.allowRequest()){
            return 0;
        }
    }
}
