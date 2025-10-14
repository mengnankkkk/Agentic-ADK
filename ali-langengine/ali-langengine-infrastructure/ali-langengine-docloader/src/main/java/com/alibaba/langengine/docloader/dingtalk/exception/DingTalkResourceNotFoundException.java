package com.alibaba.langengine.docloader.dingtalk.exception;


public class DingTalkResourceNotFoundException extends DingTalkException {

    private final String resourceType;
    private final String resourceId;

    public DingTalkResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found: %s", resourceType, resourceId), "RESOURCE_NOT_FOUND", 404);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public DingTalkResourceNotFoundException(String message, String resourceType, String resourceId) {
        super(message, "RESOURCE_NOT_FOUND", 404);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
