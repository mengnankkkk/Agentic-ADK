package com.alibaba.langengine.docloader.feishu.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuResult<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 分页信息
     */
    private FeishuMeta meta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeishuMeta {

        /**
         * 下一页标识
         */
        @JsonProperty("page_token")
        private String pageToken;

        /**
         * 是否还有更多数据
         */
        @JsonProperty("has_more")
        private Boolean hasMore;

        /**
         * 总数量（某些API返回）
         */
        private Integer total;
    }
}