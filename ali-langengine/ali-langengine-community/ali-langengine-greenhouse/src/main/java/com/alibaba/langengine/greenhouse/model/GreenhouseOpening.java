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
 * Greenhouse Job Opening
 * 
 * @author LangEngine Team
 */
@Data
public class GreenhouseOpening {
    
    @JSONField(name = "id")
    private Long id;
    
    @JSONField(name = "opening_id")
    private String openingId;
    
    @JSONField(name = "status")
    private String status;
    
    @JSONField(name = "opened_at")
    private String openedAt;
    
    @JSONField(name = "closed_at")
    private String closedAt;
    
    @JSONField(name = "application_id")
    private Long applicationId;
    
    @JSONField(name = "close_reason")
    private GreenhouseCloseReason closeReason;
}
