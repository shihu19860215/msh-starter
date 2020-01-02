package com.msh.starter.redis.cache;

import com.msh.frame.common.util.StringUtil;
import com.msh.frame.interfaces.ICache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class RedisCache<K,V> implements ICache<K,V> {
    private final RedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisCache(RedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix + StringUtil.COLON;
    }

    @Override
    public V get(K k) {
        return (V)redisTemplate.opsForValue().get(keyPrefix+k);
    }

    @Override
    public void put(K k, V o) {
        redisTemplate.opsForValue().set(keyPrefix+k,o);
    }

    @Override
    public void put(K k, V o, long expireSecond) {
        redisTemplate.opsForValue().set(keyPrefix+k,o,expireSecond,TimeUnit.SECONDS);
    }

    @Override
    public boolean hasKey(K k) {
        return redisTemplate.hasKey(keyPrefix+k);
    }

    @Override
    public void remove(K k) {
        redisTemplate.delete(keyPrefix+k);
    }


    @Override
    public void remove(Collection c) {
        Set<String> set=new HashSet<>();
        Iterator it=c.iterator();
        while(it.hasNext()){
            set.add(keyPrefix+ String.valueOf(it.next()));
        }
        redisTemplate.delete(set);
    }

    @Override
    public void clearPrefix(K k) {
        redisTemplate.delete(redisTemplate.keys(keyPrefix+k+"*"));
    }

    @Override
    public void clear(){
        Set set=redisTemplate.keys(keyPrefix+StringUtil.ASTERISK);
        if(null!=set) {
            redisTemplate.delete(set);
        }
    }

}
