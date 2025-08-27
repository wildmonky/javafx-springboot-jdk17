package org.lizhao.validator.spring.event;

import org.springframework.context.ApplicationEvent;

public class DeviceFlushEvent extends ApplicationEvent {

    public DeviceFlushEvent(Object source) {
        super(source);
    }

}
