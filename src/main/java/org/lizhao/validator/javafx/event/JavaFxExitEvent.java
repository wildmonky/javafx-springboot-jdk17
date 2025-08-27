package org.lizhao.validator.javafx.event;

import java.util.EventObject;

public class JavaFxExitEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public JavaFxExitEvent(Object source) {
        super(source);
    }
}
