package com.msh.starter.common.base;

public interface IClearListCache<Q> {
    /**
     * 清空list缓存
     * @param q
     */
    void clearListCache(Q q);

    /**
     * 清空获取list单条信息的缓存
     * @param q
     */
    void clearListOneCache(Q q);
}
