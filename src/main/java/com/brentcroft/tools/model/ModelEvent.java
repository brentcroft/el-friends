package com.brentcroft.tools.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ModelEvent
{
    private EventType eventType;
    private Model source;
    private String message;
    private Throwable exception;

    public enum EventType
    {
        MESSAGE,
        EXCEPTION,
        WHILE_DO_TEST,
        WHILE_DO_OPERATION,
        STEPS_START,
        STEP_START;

        public ModelEvent newEvent(Model source, String message) {
            return newEvent(source, message, null);
        }
        public ModelEvent newEvent(Model source, String message, Throwable exception) {
            return new ModelEvent(this, source, message, exception);
        }
    }
}
