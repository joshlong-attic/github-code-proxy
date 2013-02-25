package com.joshlong.ghproxy;

import com.joshlong.ghproxy.jsonp.JsonpCallbackHandlerMethodArgumentResolver;
import com.joshlong.ghproxy.jsonp.JsonpContextHandlerMethodArgumentResolver;
import com.joshlong.ghproxy.jsonp.JsonpMappingJacksonHttpMessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;


@Configuration
@EnableWebMvc
public class WebMvcConfiguration
    extends WebMvcConfigurerAdapter
    implements BeanFactoryAware
{

    private ConfigurableBeanFactory beanFactory;

    @Bean
    public GithubProxyController githubProxyController() {
        return new GithubProxyController();
    }

    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
       assert this.beanFactory != null ;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        assert this.beanFactory != null  : "the beanFactory can't be null" ;
        argumentResolvers.add( new JsonpContextHandlerMethodArgumentResolver()) ;
        argumentResolvers.add(new JsonpCallbackHandlerMethodArgumentResolver( this.beanFactory ));
    }


    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JsonpMappingJacksonHttpMessageConverter());
    }
}