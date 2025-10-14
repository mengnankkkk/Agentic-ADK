package com.alibaba.langengine.docloader.dingtalk.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DingTalkResult<T> {

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应元数据
     */
    private DingTalkMeta meta;

    /**
     * 错误码
     */
    private String errcode;

    /**
     * 错误信息
     */
    private String errmsg;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应元数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DingTalkMeta {

        /**
         * 总记录数
         */
        private Integer total;

        /**
         * 当前页码
         */
        private Integer page;

        /**
         * 每页大小
         */
        private Integer size;

        /**
         * 是否有下一页
         */
        private Boolean hasMore;

        /**
         * 下一页游标
         */
        private String nextCursor;
    }
}