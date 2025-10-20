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
package com.alibaba.langengine.dashvector.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DashVectorParamTest {

    @Test
    public void testDefaultValues() {
        DashVectorParam param = new DashVectorParam();
        
        assertEquals(1536, param.getDimension());
        assertEquals("cosine", param.getMetric());
        assertEquals(1, param.getReplicas());
        assertEquals(1, param.getShards());
        assertEquals(100, param.getBatchSize());
        assertEquals(30000, param.getConnectTimeout());
        assertEquals(60000, param.getReadTimeout());
    }

    @Test
    public void testSettersAndGetters() {
        DashVectorParam param = new DashVectorParam();
        
        param.setDimension(768);
        assertEquals(768, param.getDimension());
        
        param.setMetric("euclidean");
        assertEquals("euclidean", param.getMetric());
        
        param.setReplicas(2);
        assertEquals(2, param.getReplicas());
        
        param.setShards(3);
        assertEquals(3, param.getShards());
        
        param.setBatchSize(50);
        assertEquals(50, param.getBatchSize());
        
        param.setConnectTimeout(15000);
        assertEquals(15000, param.getConnectTimeout());
        
        param.setReadTimeout(30000);
        assertEquals(30000, param.getReadTimeout());
    }

    @Test
    public void testBuilder() {
        DashVectorParam param = new DashVectorParam();
        param.setDimension(512);
        param.setMetric("dot_product");
        param.setBatchSize(200);
        
        assertEquals(512, param.getDimension());
        assertEquals("dot_product", param.getMetric());
        assertEquals(200, param.getBatchSize());
    }

}