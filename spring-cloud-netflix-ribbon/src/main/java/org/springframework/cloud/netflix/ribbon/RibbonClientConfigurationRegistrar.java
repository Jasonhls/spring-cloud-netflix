/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * 在@RibbonClient注解和@RibbonClients注解上被import到Spring容器中了
 */
public class RibbonClientConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		Map<String, Object> attrs = metadata
				.getAnnotationAttributes(RibbonClients.class.getName(), true);
		if (attrs != null && attrs.containsKey("value")) {
			//获取@RibbonClients注解中的value，是多个@RibbonClient
			AnnotationAttributes[] clients = (AnnotationAttributes[]) attrs.get("value");
			for (AnnotationAttributes client : clients) {
				//获取@RibbonClient注解属性configuration上的配置并注册为bean
				registerClientConfiguration(registry, getClientName(client),
						client.get("configuration"));
			}
		}
		if (attrs != null && attrs.containsKey("defaultConfiguration")) {
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			}
			else {
				name = "default." + metadata.getClassName();
			}
			//获取@RibbonClients注解属性defaultConfiguration上的配置并注册为bean, bean名称以default.开头
			registerClientConfiguration(registry, name,
					attrs.get("defaultConfiguration"));
		}
		Map<String, Object> client = metadata
				.getAnnotationAttributes(RibbonClient.class.getName(), true);
		String name = getClientName(client);
		if (name != null) {
			//获取@RibbonClient注解属性configuration上的配置并注册为bean
			registerClientConfiguration(registry, name, client.get("configuration"));
		}
	}

	private String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = (String) client.get("value");
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}
		throw new IllegalStateException(
				"Either 'name' or 'value' must be provided in @RibbonClient");
	}

	private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
			Object configuration) {
		//将RibbonClientSpecification注册到Spring容器中
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(RibbonClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + ".RibbonClientSpecification",
				builder.getBeanDefinition());
	}

}
