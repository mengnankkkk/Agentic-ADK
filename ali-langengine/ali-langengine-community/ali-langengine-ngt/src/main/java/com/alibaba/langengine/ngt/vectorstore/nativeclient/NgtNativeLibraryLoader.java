package com.alibaba.langengine.ngt.vectorstore.nativeclient;

import com.sun.jna.Native;

public final class NgtNativeLibraryLoader {

    private NgtNativeLibraryLoader() {
    }

    public static NgtNativeLibrary load(String libraryName) {
        return Native.load(libraryName, NgtNativeLibrary.class);
    }
}
