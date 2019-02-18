package com.github.lyd.msg.producer.configuration;

import com.github.lyd.msg.producer.locator.MailSenderLocator;
import com.github.lyd.msg.producer.service.MailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * @author liuyadu
 */
@Configuration
@EnableConfigurationProperties({MailChannelsProperties.class})
public class MailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MailSenderLocator.class)
    public MailSenderLocator mailSenderLocator(MailChannelsProperties properties) {
        MailSenderLocator locator = new MailSenderLocator(properties);
        locator.setMailSenders(locator.locateSenders());
        return locator;
    }


    @Bean
    public MailSender mailSender(FreeMarkerConfigurer freeMarkerConfigurer) {
        MailSender mailSender = new MailSender();
        mailSender.setFreeMarkerConfigurer(freeMarkerConfigurer);
        return mailSender;
    }
}