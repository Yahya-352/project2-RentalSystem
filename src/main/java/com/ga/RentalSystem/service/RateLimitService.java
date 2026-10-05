package com.ga.RentalSystem.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RateLimitService {
    private final Map<String , TokenBucket> buckets = new HashMap<>();

    public Integer checkLimit(String key , double capacity , double window){
        double refillRate = capacity / window;

        buckets.putIfAbsent(key,new TokenBucket((int) capacity, refillRate));
        TokenBucket bucket = buckets.get(key);

        if(bucket.allowRequest()){
            return 0;
        }
        return (int) Math.ceil(bucket.secondsUntilNextToken());
    }
}
