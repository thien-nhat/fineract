/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.infrastructure.core.jersey;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import org.apache.fineract.infrastructure.core.api.jersey.PageableParamProvider;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    private final String serverContextPath;

    JerseyConfig(@Value("${server.servlet.context-path:}") String serverContextPath) {
        this.serverContextPath = normalizeContextPath(serverContextPath);
        register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
        register(new AbstractBinder() {

            @Override
            protected void configure() {
                bind(PageableParamProvider.class).to(ValueParamProvider.class).in(Singleton.class);
            }
        });
        register(org.glassfish.jersey.server.validation.ValidationFeature.class);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }

    @Autowired
    ApplicationContext appCtx;

    @PostConstruct
    public void setup() {
        register(new OpenApiResource().openApiConfiguration(new SwaggerConfiguration()
                .openAPI(new OpenAPI().addServersItem(new Server().url(serverContextPath))
                        .components(new Components()
                                .addSecuritySchemes("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"))
                                .addSecuritySchemes("tenantid",
                                        new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER)
                                                .name("Fineract-Platform-TenantId")))
                        .addSecurityItem(new SecurityRequirement().addList("basicAuth").addList("tenantid")))
                .resourcePackages(Set.of("org.apache.fineract")).readerClass("org.apache.fineract.infrastructure.openapi.FineractOperationIdReader")));

        appCtx.getBeansWithAnnotation(Path.class).values().forEach(this::register);

        appCtx.getBeansWithAnnotation(Provider.class).values().forEach(this::register);
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }
}
