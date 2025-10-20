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

import java.util.List;

/**
 * Greenhouse Candidate
 * 
 * @author LangEngine Team
 */
@Data
public class GreenhouseCandidate {
    
    @JSONField(name = "id")
    private Long id;
    
    @JSONField(name = "first_name")
    private String firstName;
    
    @JSONField(name = "last_name")
    private String lastName;
    
    @JSONField(name = "company")
    private String company;
    
    @JSONField(name = "title")
    private String title;
    
    @JSONField(name = "created_at")
    private String createdAt;
    
    @JSONField(name = "updated_at")
    private String updatedAt;
    
    @JSONField(name = "last_activity")
    private String lastActivity;
    
    @JSONField(name = "is_private")
    private Boolean isPrivate;
    
    @JSONField(name = "photo_url")
    private String photoUrl;
    
    @JSONField(name = "attachments")
    private List<GreenhouseAttachment> attachments;
    
    @JSONField(name = "application_ids")
    private List<Long> applicationIds;
    
    @JSONField(name = "phone_numbers")
    private List<GreenhousePhoneNumber> phoneNumbers;
    
    @JSONField(name = "email_addresses")
    private List<GreenhouseEmailAddress> emailAddresses;
    
    @JSONField(name = "addresses")
    private List<GreenhouseAddress> addresses;
    
    @JSONField(name = "website_addresses")
    private List<GreenhouseWebsiteAddress> websiteAddresses;
    
    @JSONField(name = "social_media_addresses")
    private List<GreenhouseSocialMediaAddress> socialMediaAddresses;
    
    @JSONField(name = "recruiter")
    private GreenhouseUser recruiter;
    
    @JSONField(name = "coordinator")
    private GreenhouseUser coordinator;
    
    @JSONField(name = "tags")
    private List<String> tags;
}
