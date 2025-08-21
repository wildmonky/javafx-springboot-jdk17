package org.lizhao.validator.event;

import org.springframework.context.ApplicationEvent;

public class DeviceFlushEvent extends ApplicationEvent {
    public DeviceFlushEvent(Object source) {
        super(source);
    }
}
