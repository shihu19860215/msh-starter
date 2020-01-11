package com.msh.starter.redis.config;

import com.alibaba.fastjson.parser.ParserConfig;
import com.msh.starter.redis.common.FastJson2JsonRedisSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {



    @Bean
    public RedisSerializer fastJson2JsonRedisSerializer() {
        ParserConfig.getGlobalInstance().addAccept("com.");
        return new FastJson2JsonRedisSerializer<Object>(Object.class);
    }

    /**
     * RedisTemplate配置
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory, RedisSerializer fastJson2JsonRedisSerializer) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.setValueSerializer(fastJson2JsonRedisSerializer);
        template.setHashValueSerializer(fastJson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;

      /*  //定义key序列化方式
        //RedisSerializer<String> redisSerializer = new StringRedisSerializer();//Long类型会出现异常信息;需要我们上面的自定义key生成策略，一般没必要
        //定义value的序列化方式
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // template.setKeySerializer(redisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;*/
    }
}
