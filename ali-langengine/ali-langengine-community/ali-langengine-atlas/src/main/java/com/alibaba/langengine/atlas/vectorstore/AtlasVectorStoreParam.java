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
package com.alibaba.langengine.atlas.vectorstore;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtlasVectorStoreParam {

    /**
     * The name of the field that contains the vector embeddings.
     */
    @Builder.Default
    private String vectorPath = "embedding";

    /**
     * The name of the field that contains the document's text content.
     */
    @Builder.Default
    private String textPath = "text";

    /**
     * The name of the field that contains the document's metadata.
     */
    @Builder.Default
    private String metadataPath = "metadata";

    /**
     * The name of the field that contains the document's unique id.
     */
    @Builder.Default
    private String uniqueIdPath = "_id";

    /**
     * The number of candidate documents to consider during the search.
     */
    @Builder.Default
    private int numCandidates = 150;
}