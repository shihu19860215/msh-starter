package com.msh.starter.id.generate.config;

import com.msh.frame.interfaces.IdGenerateable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.msh.starter.id.generate")
@ConditionalOnMissingBean(IdGenerateable.class)
public class IdGenerateAutoConfiguration {
}
