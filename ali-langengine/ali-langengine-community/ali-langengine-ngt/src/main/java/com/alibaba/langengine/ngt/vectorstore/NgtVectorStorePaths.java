package com.alibaba.langengine.ngt.vectorstore;

import java.nio.file.Path;
import java.nio.file.Paths;

final class NgtVectorStorePaths {

    private static final String DEFAULT_INDEX_DIR = System.getProperty("user.home", ".") + "/.ngt/index";

    private NgtVectorStorePaths() {
    }

    static String defaultIndexPath() {
        return normalize(DEFAULT_INDEX_DIR);
    }

    static String normalize(String path) {
        return Paths.get(path).toAbsolutePath().toString();
    }

    static Path asPath(String path) {
        return Paths.get(path).toAbsolutePath();
    }
}
