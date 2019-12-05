package com.msh.starter.redis.cache;

import com.msh.frame.interfaces.ICache;
import com.msh.frame.interfaces.ICacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RedisCacheMangger implements ICacheManager {
    private Map<String,ICache> redisCacheMap=new ConcurrentHashMap<>();
    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public <V> ICache<V> getCache(String name) {
        if(redisCacheMap.containsKey(name)){
            return redisCacheMap.get(name);
        }
        //访问量不大,直接使用synchronized代码块
        synchronized (this){
            if(redisCacheMap.containsKey(name)){
                return redisCacheMap.get(name);
            }
            ICache cache=new RedisCache(redisTemplate,name);
            redisCacheMap.put(name,cache);
            return cache;
        }
    }
}
