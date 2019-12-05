package com.msh.starter.redis.common;

import com.msh.frame.interfaces.IdGenerateable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 乐观redis锁，只通过redis获取锁，不加线程锁
 * 调用：
 RedisLock lock = new RedisLock(redisTemplate, key, 10000, 20000);
 boolean hasLock=false;
 try {
    hasLock=lock.lock();
     if(hasLock) {
     //需要加锁的代码
     }
 }
 } catch (InterruptedException e) {
 e.printStackTrace();
 }finally {
    if(hasLock){
        lock.unlock();
    }
 }
 */
public class OptimisticRedisLock {
    private static Logger LOGGER = LoggerFactory.getLogger(OptimisticRedisLock.class);
    final private static String PERFIX_LOCK_KEY = "lock_key:";
    /**
     * 使用线程空间存储一个线程唯一id
     */
    private ThreadLocal<Long> threadLocal = new ThreadLocal();
    private RedisTemplate redisTemplate;
    private IdGenerateable idGenerateable;

    /**
     * 循环获取锁时默认等待时间
     */
    private static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 100;

    /**
     * 锁超时时间，防止线程在入锁以后，无限的执行等待
     * 毫秒单位
     */
    private int expireMsecs = 60 * 1000;

    /**
     * 尝试获取锁等待时间，防止一直尝试
     * 毫秒单位
     */
    private int tryTimeout = 10 * 1000;

    /**
     * 是否一致尝试
     */
    private boolean tryAlways=true;

    /**
     * key 序列化方式
     */
    private RedisSerializer keySerializer;
    /**
     * value 序列化方式
     */
    private RedisSerializer valueSerializer;

    /**
     * 创建redis锁对象 使用默认的60秒超时,并一直重试直到获取锁
     * @param redisTemplate
     */
    public OptimisticRedisLock(RedisTemplate redisTemplate, IdGenerateable idGenerateable) {
        this.redisTemplate = redisTemplate;
        keySerializer=redisTemplate.getKeySerializer();
        valueSerializer=redisTemplate.getValueSerializer();
        this.idGenerateable=idGenerateable;
    }

    /**
     * 创建redis锁对象 ,使用指定锁超时时间 ,并一直重试直到获取锁
     * @param redisTemplate
     * @param expireMsecs 锁超时时间
     */
    public OptimisticRedisLock(RedisTemplate redisTemplate, int expireMsecs, IdGenerateable idGenerateable) {
        this(redisTemplate,idGenerateable);
        this.expireMsecs = expireMsecs;
    }

    /**
     * 创建redis锁对象 ,使用指定锁超时时间 ,并设置重试时间
     * @param redisTemplate
     * @param expireMsecs 锁超时时间
     * @param tryTimeout 重试多久来获取锁
     */
    public OptimisticRedisLock(RedisTemplate redisTemplate, int expireMsecs, int tryTimeout, IdGenerateable idGenerateable) {
        this(redisTemplate, expireMsecs,idGenerateable);
        this.tryTimeout = tryTimeout;
        this.tryAlways=false;
    }

    private boolean setNX(final String key, final Long value) {
        Object obj = null;
        try {
            byte[] keyByte=keySerializer.serialize(key);
            obj = redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    Boolean success = connection.setNX(keyByte, valueSerializer.serialize(value));
                    if (null != success && success) {
                        connection.pExpire(keyByte,expireMsecs);
                    }else{
                        //防止连接超时导致永久不过期
                        Long ttl = connection.ttl(keyByte);
                        if (null != ttl && -1 == ttl) {
                            connection.pExpire(keyByte,expireMsecs);
                        }
                    }
                    connection.close();
                    return success;
                }
            });
        } catch (Exception e) {
            LOGGER.warn("setNX redis error, key : {}", key);
        }
        return obj != null ? (Boolean) obj : false;
    }

    /**
     * 获得 lock.
     * 实现思路: 主要是使用了redis 的setnx命令,缓存了锁.
     * reids缓存的key是锁的key,所有的共享, value是锁的拥有者
     * 执行过程:
     * 1.通过setnx尝试设置某个key的值,成功(当前没有这个锁)则返回,成功获得锁
     * 2.锁已经存在则等待锁释放或过期
     *
     */
    public boolean  lock(String lockKey){
        lockKey = PERFIX_LOCK_KEY + lockKey;
        //获取当前线程唯一的id
        Long value=getThreadUID();
        if(tryAlways){
                while(true){
                    //设置lockKey的锁值，如果设置成功则获取 该锁，锁创建成功
                    if (this.setNX(lockKey, value)) {
                        return true;
                    }
                    /*
                        延迟100 毫秒,  防止无端消耗cpu
                     */
                    try {
                        Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
        }else {
            try {
                long timeout = tryTimeout;
                long tryLockTimeStart=System.currentTimeMillis();
                timeout =timeout - System.currentTimeMillis() + tryLockTimeStart;
                while (timeout >= 0) {
                    //设置lockKey的锁值，如果设置成功则获取 该锁，锁创建成功
                    if (this.setNX(lockKey, value)) {
                        return true;
                    }
                    timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;

                    /*
                        延迟100 毫秒,  防止无端消耗cpu
                     */
                    try {
                        Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }catch (Exception e){
            }
        }
        return false;
    }

    /**
     * 释放redis锁
     * 查看当前锁是否为自己所有，
     * 自己所有的锁才能释放
     * 线程过多的情况下回出现连接超时，导致无法释放该锁，只能等待超时后自动释放
     */
    public void unlock(String lockKey) {
        Object obj = null;
        byte[] key=keySerializer.serialize(PERFIX_LOCK_KEY + lockKey);
        try {
            obj = redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    connection.watch(key);
                    byte[] valueByte=connection.get(key);
                    if(null==valueByte){
                        connection.unwatch();
                        connection.close();
                        return null;
                    }
                    Long value= (Long) valueSerializer.deserialize(valueByte);
                    if(isOwnerThread(value)){
                        connection.multi();
                        connection.del(key);
                        List list=connection.exec();
                        connection.close();
                        return list;
                    }
                    connection.unwatch();
                    connection.close();
                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.warn("watch and delete redis error, key : {}", lockKey);
        }finally {
            threadLocal.remove();
        }
    }

    /**
     * 续约 ，设置属性中的过期时间
     * 在获取锁时使用
     */
    public void renewContractExpire(String lockKey){
        redisTemplate.expire(PERFIX_LOCK_KEY + lockKey , expireMsecs ,TimeUnit.MILLISECONDS);
    }

    /**
     * 获取线程唯一id
     * @return
     */
    public Long getThreadUID(){
        Long value = threadLocal.get();
        if (null == value) {
            synchronized (this){
                value = threadLocal.get();
                if (null == value) {
                    value = idGenerateable.getUniqueID();
                    threadLocal.set(value);
                }
            }
        }
        return value;
    }

    /**
     * 判断是否为当前线程id
     * @param value
     * @return
     */
    public boolean isOwnerThread(Long value){
        return getThreadUID().equals(value);
    }

}
