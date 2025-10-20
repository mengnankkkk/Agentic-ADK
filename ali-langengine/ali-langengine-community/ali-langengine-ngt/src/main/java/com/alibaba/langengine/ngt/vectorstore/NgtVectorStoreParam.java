package com.alibaba.langengine.ngt.vectorstore;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;


@Data
@Builder
public class NgtVectorStoreParam {

    /**
     * Absolute path to the NGT index directory.
     */
    @Builder.Default
    private String indexPath = NgtVectorStorePaths.defaultIndexPath();

    /**
     * Native library name loaded via JNA.
     */
    @Builder.Default
    private String nativeLibraryName = "ngt";

    /**
     * Expected vector dimension. Required when creating a new index.
     */
    @Builder.Default
    private int dimension = 1536;

    /**
     * Distance type used by the index.
     */
    @Builder.Default
    private DistanceType distanceType = DistanceType.COSINE;

    /**
     * Default number of nearest neighbors returned in search.
     */
    @Builder.Default
    private int searchK = 10;

    /**
     * Search epsilon controlling approximation accuracy.
     */
    @Builder.Default
    private float searchEpsilon = 0.1f;

    /**
     * Search radius. Use negative value for unlimited.
     */
    @Builder.Default
    private float searchRadius = -1.0f;

    /**
     * When true, triggers automatic index rebuild after inserts.
     */
    @Builder.Default
    private boolean autoBuildIndex = true;

    /**
     * Validate configuration.
     */
    public void validate() {
        if (StringUtils.isBlank(indexPath)) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                    "indexPath cannot be blank");
        }
        if (StringUtils.isBlank(nativeLibraryName)) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                    "nativeLibraryName cannot be blank");
        }
        if (dimension <= 0) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                    "dimension must be greater than 0");
        }
        if (searchK <= 0) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                    "searchK must be greater than 0");
        }
        if (searchRadius == 0) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                    "searchRadius cannot be zero");
        }
    }

    public enum DistanceType {
        L2(1),
        ANGULAR(2),
        COSINE(3);

        private final int nativeCode;

        DistanceType(int nativeCode) {
            this.nativeCode = nativeCode;
        }

        public int getNativeCode() {
            return nativeCode;
        }
    }

    public int distanceTypeCode() {
        return distanceType.getNativeCode();
    }
}
