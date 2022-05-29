package com.hquyyp.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
/**
 * <p> 
 *     第三方接口请求服务模板类配置类
 * </p> 
 * @author liulingyu
 * @date 2022-05-24 17:11
 * @version 1.0
 */
@Configuration
public class RestTemplateConfig {
    @Bean

    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }
}
