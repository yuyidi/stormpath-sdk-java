/*
 * Copyright 2015 Stormpath, Inc.
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
package com.stormpath.sdk.impl.ds.cache;

import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiKeyList;
import com.stormpath.sdk.cache.Cache;
import com.stormpath.sdk.impl.ds.DefaultResourceDataResult;
import com.stormpath.sdk.impl.ds.Filter;
import com.stormpath.sdk.impl.ds.FilterChain;
import com.stormpath.sdk.impl.ds.ResourceDataRequest;
import com.stormpath.sdk.impl.ds.ResourceDataResult;
import com.stormpath.sdk.impl.http.CanonicalUri;
import com.stormpath.sdk.impl.http.QueryString;
import com.stormpath.sdk.impl.resource.CollectionProperties;
import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.Collections;
import com.stormpath.sdk.resource.CollectionResource;
import com.stormpath.sdk.resource.Resource;

import java.util.Map;

import static com.stormpath.sdk.impl.api.ApiKeyParameter.*;
import static com.stormpath.sdk.impl.resource.AbstractCollectionResource.*;

public class ReadCacheFilter implements Filter {

    private final String baseUrl;
    private final CacheResolver cacheResolver;

    public ReadCacheFilter(String baseUrl, CacheResolver cacheResolver) {
        Assert.hasText(baseUrl, "baseUrl cannot be null or empty.");
        Assert.notNull(cacheResolver, "cacheResolver cannot be null.");
        this.baseUrl = baseUrl;
        this.cacheResolver = cacheResolver;
    }

    @Override
    public ResourceDataResult filter(ResourceDataRequest request, FilterChain chain) {

        if (isCacheRetrievalEnabled(request)) {
            ResourceDataResult result = getCachedResourceData(request);
            if (result != null) {
                return result;
            }
        }

        //cache miss - let the chain continue:
        return chain.filter(request);
    }

    private ResourceDataResult getCachedResourceData(ResourceDataRequest request) {

        final CanonicalUri uri = request.getUri();
        final String href = uri.getAbsolutePath();
        final QueryString query = uri.getQuery();
        final Class<? extends Resource> clazz = request.getResourceClass();

        Map<String, ?> data = null;

        if (isApiKeyCollectionQuery(request)) {

            String cacheHref = baseUrl + "/apiKeys/" + query.get(ID.getName());
            Class<ApiKey> cacheClass = com.stormpath.sdk.api.ApiKey.class;

            Map<String, ?> apiKeyData = getCachedValue(cacheHref, cacheClass);

            if (!Collections.isEmpty(apiKeyData)) {
                int offset = getValue(query, OFFSET.getName(), 0);
                int limit = getValue(query, LIMIT.getName(), 25);
                data = new CollectionProperties.Builder().setHref(href).setOffset(offset).setLimit(limit)
                                                         .setItemsMap(apiKeyData).build();
            }
        } else {
            data = getCachedValue(href, clazz);
        }

        if (Collections.isEmpty(data)) {
            return null;
        }

        return new DefaultResourceDataResult(uri, coerce(data), clazz);
    }

    private int getValue(QueryString query, String propName, int defaultValue) {
        return query.containsKey(propName) ? Integer.valueOf(query.get(propName)) : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> coerce(Map<String, ?> data) {
        return (Map<String, Object>) data;
    }

    private boolean isApiKeyCollectionQuery(ResourceDataRequest request) {
        return ApiKeyList.class.isAssignableFrom(request.getResourceClass()) &&
               request.getUri().hasQuery() && request.getUri().getQuery().containsKey(ID.getName());
    }

    private boolean isCacheRetrievalEnabled(ResourceDataRequest request) {
        //we currently don't cache CollectionResources themselves (only their internal instance resources).  So we
        //return false in this case so a new cache region isn't auto created unnecessarily
        //(cacheManager.getCache(name) will auto-create a region if called and it does not yet exist)
        return !CollectionResource.class.isAssignableFrom(request.getResourceClass());
    }

    private Map<String, ?> getCachedValue(String href, Class<? extends Resource> clazz) {
        Assert.hasText(href, "href argument cannot be null or empty.");
        Assert.notNull(clazz, "Class argument cannot be null.");
        Cache<String, Map<String, ?>> cache = getCache(clazz);
        return cache.get(href);
    }

    private <T> Cache<String, Map<String, ?>> getCache(Class<T> clazz) {
        return this.cacheResolver.getCache(clazz);
    }
}
