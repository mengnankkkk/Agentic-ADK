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
package com.alibaba.langengine.greenhouse.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Greenhouse Address
 * 
 * @author LangEngine Team
 */
@Data
public class GreenhouseAddress {
    
    @JSONField(name = "value")
    private String value;
    
    @JSONField(name = "type")
    private String type;
}
