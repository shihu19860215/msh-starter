package com.msh.starter.common.base;

import com.msh.frame.client.base.BasePO;
import com.msh.frame.client.base.BaseQO;
import com.msh.frame.client.base.BaseServiceImpl;
import com.msh.frame.client.common.CommonResult;
import com.msh.frame.interfaces.ICache;
import com.msh.frame.interfaces.ICacheManager;
import com.msh.starter.common.instance.ApplicationContextUtil;

import java.util.*;

/**
 * @author shihu
 * 带缓存的serviceImpl
 * 永久缓存单条信息和list信息，分页信息都缓存
 * 一些数量少且修改非常少的数据可以使用
 * 虽然是永久缓存，但还是设置了过期时间30天
 * 防止不使用之后redis一直占用内存
 * @param <T>
 * @param <Q>
 */
public abstract class BasePermanentCacheServiceImpl<T extends BasePO, Q extends BaseQO> extends BaseServiceImpl<T,Q> {
    protected ICache<String,CommonResult> cache;
    /**
     * 单条查询缓存
     */
    final private static String GET_PREFIX="get:";
    /**
     * 列表缓存
     */
    final private static String LIST_PREFIX="list:";
    /**
     * 获取列表查询单条信息缓存
     */
    final private static String LIST_ONE_PREFIX="list_one:";
    /**
     * 查询条数缓存
     */
    final private static String COUNT_PREFIX="count:";
    /**
     *  非空值过期时间
     */
    final private static long NO_NULL_EXPIRE_SECOND=30*24*60*60L;
    /**
     * 空值过期时间
     */
    final private static long NULL_EXPIRE_SECOND=300L;

    @Override
    public CommonResult<T> get(long param) {
        String key = GET_PREFIX+ param;
        CommonResult commonResult=getCache().get(key);
        if(null==commonResult){
            commonResult=super.get(param);
            if(commonResult.isSuccess()){
                if(null!=commonResult.getResult()){
                    getCache().put(key,commonResult,NO_NULL_EXPIRE_SECOND);
                }else {
                    getCache().put(key,commonResult,NULL_EXPIRE_SECOND);
                }
            }
        }
        return commonResult;
    }

    @Override
    public CommonResult<Boolean> update(T param) {
        String key = GET_PREFIX+ param.getId();
        getCache().remove(key);
        clearListCountCache();
        return super.update(param);
    }

    @Override
    public CommonResult<Boolean> delete(long param) {
        String key = GET_PREFIX+ param;
        getCache().remove(key);
        clearListCountCache();
        return super.delete(param);
    }

    @Override
    public CommonResult<Boolean> insert(T param) {
        String key=GET_PREFIX+param.getId();
        getCache().remove(key);
        clearListCountCache();
        return super.insert(param);
    }

    @Override
    public CommonResult<Boolean> insertCollection(Collection param) {
        Set<String> set=new HashSet<>();
        Iterator it=param.iterator();
        while(it.hasNext()){
            T t= (T) it.next();
            set.add(GET_PREFIX+t.getId());
        }
        getCache().remove(set);
        clearListCountCache();
        return super.insertCollection(param);
    }


    private void clearListCountCache(){
        getCache().clearPrefix(LIST_PREFIX);
        getCache().clearPrefix(LIST_ONE_PREFIX);
        getCache().clearPrefix(COUNT_PREFIX);
    }

    @Override
    public CommonResult<List<T>> list(Q param) {
        String key = LIST_PREFIX + param.toString();
        CommonResult<List<T>> commonResult= getCache().get(key);
        if(null==commonResult){
            commonResult = super.list(param);
            if(commonResult.isSuccess()){
                getCache().put(key,commonResult,NO_NULL_EXPIRE_SECOND);
                if(null!=param.getPage()){
                    String countKey = COUNT_PREFIX + param.toString();
                    getCache().put(countKey,CommonResult.successReturn(commonResult.getCount()),NO_NULL_EXPIRE_SECOND);
                }
            }
        }
        return commonResult;
    }

    @Override
    public CommonResult listOne(Q param) {
        String key=LIST_ONE_PREFIX+param.toString();
        CommonResult<T> commonResult= (CommonResult<T>) getCache().get(key);
        if(null==commonResult){
            commonResult=super.listOne(param);
            if(commonResult.isSuccess()){
                getCache().put(key,commonResult,NO_NULL_EXPIRE_SECOND);
            }
        }
        return commonResult;
    }

    @Override
    public CommonResult<Integer> count(Q param) {
        String key=COUNT_PREFIX+param.toString();
        CommonResult<Integer> commonResult= (CommonResult<Integer>) getCache().get(key);
        if(null == commonResult){
            commonResult = super.count(param);
            if(commonResult.isSuccess()){
                getCache().put(key,commonResult,NO_NULL_EXPIRE_SECOND);
            }
        }
        return super.count(param);
    }

    public ICache<String,CommonResult> getCache(){
        if(null==cache){
            cache=ApplicationContextUtil.getBean(ICacheManager.class).getCache(this.getClass().getName());
        }
        return cache;
    }

}
