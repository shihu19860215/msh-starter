package com.msh.starter.common.base;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.msh.frame.client.base.BasePO;
import com.msh.frame.client.base.BaseQO;
import com.msh.frame.client.common.CommonResult;

import java.util.List;

/**
 * @author shihu
 * 带缓存的serviceImpl
 * 缓存单条信息和list信息，分页信息都缓存
 * list缓存有时效，如果需要实时信息就不要使用此类
 * @param <T>
 * @param <Q>
 */
public abstract class BaseAllCacheServiceImpl<T extends BasePO, Q extends BaseQO> extends BaseCacheServiceImpl<T,Q> implements IClearListCache<Q> {
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
     * list查询结果缓存时间
     * 可以根据业务自己设置
     * 默认为3分钟
     */
    private long expireSecondTimeForList=60L*3L;
    @Override
    public CommonResult<List<T>> list(Q param) {
        String key=LIST_PREFIX+param.toString();
        CommonResult<List<T>> commonResult= (CommonResult<List<T>>) getCache().get(key);
        if(null==commonResult){
            commonResult=super.list(param);
            if (commonResult.isSuccess()){
                getCache().put(key,commonResult,expireSecondTimeForList);
                String paramStr = JSON.toJSONString(param);
                JSONObject jsonObject = JSON.parseObject(paramStr);
                jsonObject.remove("firstRow");
                jsonObject.remove("currentPage");
                jsonObject.remove("pageSize");
                String countKey = COUNT_PREFIX + jsonObject.toJSONString();
                getCache().put(countKey,CommonResult.successReturn(commonResult.getCount()),expireSecondTimeForList);
            }
        }
        return commonResult;
    }

    @Override
    public CommonResult listOne(Q param) {
        String key=LIST_ONE_PREFIX+param.toString();
        CommonResult<T> commonResult= (CommonResult<T>) getCache().get(key);
        if(null == commonResult){
            commonResult=super.listOne(param);
            if(commonResult.isSuccess()){
                getCache().put(key,commonResult,expireSecondTimeForList);
            }
        }
        return commonResult;
    }

    @Override
    public CommonResult<Integer> count(Q param) {
        String key=COUNT_PREFIX+param.toString();
        CommonResult<Integer> commonResult= (CommonResult<Integer>) getCache().get(key);
        if(null==commonResult){
            commonResult=super.count(param);
            if(commonResult.isSuccess()){
                getCache().put(key,commonResult,expireSecondTimeForList);
            }
        }
        return super.count(param);
    }

    public void setExpireSecondTimeForList(long expireSecondTimeForList) {
        this.expireSecondTimeForList = expireSecondTimeForList;
    }

    @Override
    public void clearListCache(Q baseQO) {
        getCache().remove(LIST_PREFIX+baseQO.toString());
        String paramStr = JSON.toJSONString(baseQO);
        JSONObject jsonObject = JSON.parseObject(paramStr);
        jsonObject.remove("firstRow");
        jsonObject.remove("currentPage");
        jsonObject.remove("pageSize");
        String countKey = COUNT_PREFIX + jsonObject.toJSONString();
        getCache().remove(COUNT_PREFIX + baseQO.toString());
    }

    @Override
    public void clearListOneCache(Q baseQO) {
        getCache().remove(LIST_ONE_PREFIX+baseQO.toString());
    }
}
