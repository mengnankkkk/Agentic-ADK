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
package com.alibaba.langengine.tile38;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;


public class Tile38Configuration {

    /**
     * tile38 server host
     */
    public static String TILE38_HOST = WorkPropertiesUtils.get("tile38_host", "localhost");

    /**
     * tile38 server port
     */
    public static int TILE38_PORT = Integer.parseInt(WorkPropertiesUtils.get("tile38_port", "9851"));

    /**
     * tile38 server password
     */
    public static String TILE38_PASSWORD = WorkPropertiesUtils.get("tile38_password");

    /**
     * tile38 connection timeout
     */
    public static int TILE38_TIMEOUT = Integer.parseInt(WorkPropertiesUtils.get("tile38_timeout", "5000"));

}