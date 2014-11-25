/*
 * Copyright 2014 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.servlet.filter.config;

import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.servlet.config.ConfigSingletonFactory;
import com.stormpath.sdk.servlet.filter.DefaultWrappedServletRequestFactory;
import com.stormpath.sdk.servlet.filter.WrappedServletRequestFactory;
import com.stormpath.sdk.servlet.http.Saver;

import javax.servlet.ServletContext;

public class StormpathServletRequestFactoryFactory extends ConfigSingletonFactory<WrappedServletRequestFactory> {

    @Override
    protected WrappedServletRequestFactory createInstance(ServletContext sc) throws Exception {
        Saver<AuthenticationResult> authenticationResultSaver = getConfig().getInstance("stormpath.servlet.filter.authc.saver");
        String remoteUserStrategyName = getConfig().get("stormpath.servlet.request.remoteUser.strategy");
        String userPrincipalStrategyName = getConfig().get("stormpath.servlet.request.userPrincipal.strategy");

        return new DefaultWrappedServletRequestFactory(authenticationResultSaver, userPrincipalStrategyName,
                                                         remoteUserStrategyName);
    }
}