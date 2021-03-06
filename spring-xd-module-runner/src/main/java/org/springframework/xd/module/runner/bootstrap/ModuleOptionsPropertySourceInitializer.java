/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.runner.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.xd.dirt.plugins.job.JobPluginMetadataResolver;
import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionPluginMetadataResolver;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.DelegatingModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.EnvironmentAwareModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Initialize the application context with default values for the module options.
 *  
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties(ModuleProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ModuleOptionsPropertySourceInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Autowired
	private ModuleProperties module = new ModuleProperties();

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		EnvironmentAwareModuleOptionsMetadataResolver resolver = moduleOptionsMetadataResolver();
		resolver.setEnvironment(environment);
		ModuleOptionsMetadata resolved = resolver.resolve(module.getModuleDefinition());
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (ModuleOption option : resolved) {
			map.put(option.getName(), option.getDefaultValue());
		}
		insert(environment, new MapPropertySource("moduleDefaults", map));
	}

	private void insert(ConfigurableEnvironment environment, MapPropertySource source) {
		environment.getPropertySources().addLast(source);
	}

	@Bean
	public EnvironmentAwareModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
		List<ModuleOptionsMetadataResolver> delegates = new ArrayList<ModuleOptionsMetadataResolver>();
		delegates.add(defaultResolver());
		delegates.add(new ModuleTypeConversionPluginMetadataResolver());
		delegates.add(new JobPluginMetadataResolver());
		DelegatingModuleOptionsMetadataResolver delegatingResolver = new DelegatingModuleOptionsMetadataResolver();
		delegatingResolver.setDelegates(delegates);
		EnvironmentAwareModuleOptionsMetadataResolver resolver = new EnvironmentAwareModuleOptionsMetadataResolver();
		resolver.setDelegate(delegatingResolver);
		return resolver;
	}

	@Bean
	// TODO: allow override of this
	public DefaultModuleOptionsMetadataResolver defaultResolver() {
		return new DefaultModuleOptionsMetadataResolver();
	}

}
