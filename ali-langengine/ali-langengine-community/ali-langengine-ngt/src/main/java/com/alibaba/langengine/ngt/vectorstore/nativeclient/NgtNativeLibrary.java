package com.alibaba.langengine.ngt.vectorstore.nativeclient;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public interface NgtNativeLibrary extends Library {

    Pointer ngt_create_error_object();

    void ngt_destroy_error_object(Pointer error);

    Pointer ngt_create_property(Pointer error);

    void ngt_destroy_property(Pointer property);

    boolean ngt_set_property_dimension(Pointer property, int dimension, Pointer error);

    boolean ngt_set_property_distance_type(Pointer property, int distanceType, Pointer error);

    Pointer ngt_create_graph_and_tree(String path, Pointer property, Pointer error);

    Pointer ngt_open_index(String path, Pointer error);

    void ngt_close_index(Pointer index);

    boolean ngt_save_index(Pointer index, String path, boolean replace, Pointer error);

    boolean ngt_create_index(Pointer index, int poolSize, Pointer error);

    int ngt_insert_index_as_float(Pointer index, float[] vector, int dimension, Pointer error);

    boolean ngt_remove_index(Pointer index, NativeLong objectId, Pointer error);

    Pointer ngt_create_empty_results(Pointer error);

    void ngt_destroy_results(Pointer results);

    boolean ngt_search_index_as_float(Pointer index, float[] query, int dimension, int size,
                                       float epsilon, float radius, Pointer results, Pointer error);

    int ngt_get_result_size(Pointer results, Pointer error);

    NgtObjectDistance.ByValue ngt_get_result(Pointer results, int position, Pointer error);

    String ngt_get_error_string(Pointer error);

    void ngt_clear_error_string(Pointer error);

    @com.sun.jna.Structure.FieldOrder({"id", "distance"})
    class NgtObjectDistance extends com.sun.jna.Structure {
        public int id;
        public float distance;

        public static class ByValue extends NgtObjectDistance implements com.sun.jna.Structure.ByValue {
        }
    }
}
