package com.eventhub;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
    @Test void modularStructureHasNoCycles() { ApplicationModules.of(EventHubApplication.class).verify(); }
}

