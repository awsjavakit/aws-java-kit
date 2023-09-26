package com.github.awsjavakit.dlq;

import java.util.Collection;

public interface FailedEventHandlingService {

    void handleFailedEvents(Collection<String> failedEvents);
}
