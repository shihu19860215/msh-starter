package com.msh.starter.id.generate.instance;

import com.msh.frame.common.util.IpUtil;
import com.msh.starter.id.generate.abstracts.AbstractDateIdGenerate;
import org.springframework.stereotype.Component;

/**
 * 默认实现的带日期的id生成器
 * 每秒最多生成1000个id
 * 通过获取ip后两位来区别唯一服务
 * 如果无法满足，请自己构建AbstractDateIdGenerate
 */
@Component
public class DefaultDateIdGenerate extends AbstractDateIdGenerate {
    public DefaultDateIdGenerate(){
        super(1000,100000);
    }
    @Override
    protected Integer getServerId() {
        String localIP = IpUtil.getLocalIP();
        if(null == localIP){
            throw new RuntimeException("can not get machine ip");
        }
        return 0xFFFF & IpUtil.ipStringToInterger(localIP);
    }
}
