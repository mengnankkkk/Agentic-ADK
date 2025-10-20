package com.alibaba.langengine.ngt.vectorstore.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NgtSearchResult {

    private final int objectId;
    private final float distance;
}
