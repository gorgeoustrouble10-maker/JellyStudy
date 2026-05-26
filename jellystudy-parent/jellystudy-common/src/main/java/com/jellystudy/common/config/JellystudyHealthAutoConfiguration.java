package com.jellystudy.common.config;

import com.jellystudy.common.web.JellystudyHealthController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication
@Import(JellystudyHealthController.class)
public class JellystudyHealthAutoConfiguration {
}
