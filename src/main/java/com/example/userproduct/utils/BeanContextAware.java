package com.example.userproduct.utils;

import com.example.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BeanContextAware{

    private SmsService smsService;

    public BeanContextAware(SmsService smsService) {
        this.smsService = smsService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterInit(){
        log.info("Calling send...");
        smsService.sendSms("test");
    }

}
