/**
 * Copyright (C) 2024 AIDC-AI
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
package com.alibaba.langengine.ngt;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public final class NgtConfiguration {

    public static final String NGT_LIBRARY_PATH = getConfigValue("ngt.library.path", "NGT_LIBRARY_PATH", null);
    public static final String NGT_INDEX_BASE_PATH = getConfigValue("ngt.index.base.path", "NGT_INDEX_BASE_PATH", "./data/ngt-indexes");
    public static final long NGT_COMMAND_TIMEOUT_MS = Long.parseLong(getConfigValue("ngt.command.timeout.ms",
        "NGT_COMMAND_TIMEOUT_MS", "60000"));

    private NgtConfiguration() {
    }

    private static String getConfigValue(String systemProperty, String envVar, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(envVar);
        }
        if (value == null || value.trim().isEmpty()) {
            value = defaultValue;
        }
        if (value != null && log.isDebugEnabled()) {
            log.debug("Resolved config {} (env: {}) to value: {}", systemProperty, envVar, value);
        }
        return value;
    }
}
