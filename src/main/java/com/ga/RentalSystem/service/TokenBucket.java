package com.ga.RentalSystem.service;


class TokenBucket {
    private int capacity;
    private double refillRate; // tokens per second
    private double tokens;
    private long lastRefill;

    public TokenBucket(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefill = System.nanoTime();
    }

    //everytime a user is allowed to make a request , refill the token bucket and reduce 1 token
    public synchronized boolean allowRequest() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    public double secondsUntilNextToken(){
        refill();
        if(tokens >= 1){
            return 0;
        }
        return (1- tokens) / refillRate;
    }


    private void refill() {
        long now = System.nanoTime();
        double seconds = (now - lastRefill) / 1_000_000_000.0;
        double refillTokens = seconds * refillRate;
        tokens = Math.min(capacity, tokens + refillTokens);
        lastRefill = now;
    }
}
