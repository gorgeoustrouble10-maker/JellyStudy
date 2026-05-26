package com.jellystudy.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JellystudyCorsPropertiesTest {

    @Test
    void splitsCommaSeparatedOrigins() {
        JellystudyCorsProperties props = new JellystudyCorsProperties();
        props.setAllowedOrigins("http://a.com, http://b.com");
        assertEquals(2, props.allowedOriginsArray().length);
        assertEquals("http://a.com", props.allowedOriginsArray()[0]);
        assertEquals("http://b.com", props.allowedOriginsArray()[1]);
    }
}
