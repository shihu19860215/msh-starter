package com.msh.starter.id.generate.instance;

import com.msh.frame.common.util.IpUtil;
import com.msh.frame.interfaces.IdGenerateable;
import com.msh.starter.id.generate.abstracts.AbstractIdGenerate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component("idGenerate")
public class DefaultIdGenerate extends AbstractIdGenerate {
    public DefaultIdGenerate(){
        super(16,8);
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
