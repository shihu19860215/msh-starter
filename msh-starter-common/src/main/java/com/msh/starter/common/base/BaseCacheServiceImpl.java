package com.msh.starter.common.base;


import com.msh.frame.client.base.BasePO;
import com.msh.frame.client.base.BaseQO;
import com.msh.frame.client.base.BaseServiceImpl;
import com.msh.frame.client.common.CommonResult;
import com.msh.frame.interfaces.ICache;
import com.msh.frame.interfaces.ICacheManager;
import com.msh.starter.common.instance.ApplicationContextUtil;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;


/**
 * @author shihu
 * 带缓存的serviceImpl
 * 只缓存单个一条信息，不缓存list信息
 * @param <T>
 * @param <Q>
 */
public abstract class BaseCacheServiceImpl<T extends BasePO, Q extends BaseQO> extends BaseServiceImpl<T,Q> {
    /**
     * 单条查询缓存
     */
    final private static String GET_PREFIX="get:";
    protected ICache<String,CommonResult> cache;
    /**
     * 查询到非空数据缓存时间(默认get方法过期秒数),
     * 默认12小时
     * 删除和修改数据会删除缓存
     */
    private long expireSecondTimeForGet=60*60*12;

    /**
     * 查询到空数据缓存时间(默认get方法过期秒数)
     * 默认1分钟
     * 原因 :如果缓存时间过长,查到某个id为空，insert这个id的数据后，get还是查询为空数据
     * 注:基本不会出现这类情况
     * 暂时不建议自己设置时间
     */
    final private static long DEFAULT_GET_EMPTY_EXPIRE_SECOND_TIME=60;

    @Override
    public CommonResult<T> get(long param) {
        String key=GET_PREFIX+param;
        CommonResult<T> commonResult=getCache().get(key);
        //无需使用haskey判断，因为返回结果已经被CommonResult包装，只要haskey,就不会返回null (redis的value可以存null)
        if(null==commonResult){
            commonResult=super.get(param);
            if(commonResult.isSuccess()){
                if(null==commonResult.getResult()){
                    getCache().put(key,commonResult,DEFAULT_GET_EMPTY_EXPIRE_SECOND_TIME);
                }else {
                    getCache().put(key,commonResult,expireSecondTimeForGet);
                }
            }
        }
        return commonResult;
    }

    @Override
    public CommonResult<Boolean> update(T param) {
        String key=GET_PREFIX+param.getId();
        getCache().remove(key);
        return super.update(param);
    }

    @Override
    public CommonResult<Boolean> delete(long param) {
        String key=GET_PREFIX+param;
        getCache().remove(key);
        return super.delete(param);
    }


    public ICache<String,CommonResult> getCache(){
        if(null==cache){
            ICacheManager cacheManager = ApplicationContextUtil.getBean(ICacheManager.class);
            if(null == cacheManager){
                throw new NoSuchBeanDefinitionException("can not find com.msh.frame.common.cache.CacheManger");
            }
            cache=cacheManager.getCache(this.getClass().getName());
        }
        return cache;
    }

    public void setExpireSecondTimeForGet(long expireSecondTimeForGet) {
        this.expireSecondTimeForGet = expireSecondTimeForGet;
    }
}
