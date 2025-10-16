package com.alibaba.langengine.docloader.wework.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeWorkResult<T> {

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应元数据
     */
    private WeWorkMeta meta;

    /**
     * 错误码
     * 0: 成功
     * 其他: 各种错误类型
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应时间戳
     */
    private Long timestamp;

    /**
     * 请求ID（用于追踪）
     */
    private String requestId;

    /**
     * API版本
     */
    private String apiVersion;

    /**
     * 响应元数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeWorkMeta {

        /**
         * 总记录数
         */
        private Integer total;

        /**
         * 当前偏移量
         */
        private Integer offset;

        /**
         * 当前页大小
         */
        private Integer limit;

        /**
         * 当前页码（从1开始）
         */
        private Integer page;

        /**
         * 总页数
         */
        private Integer totalPages;

        /**
         * 是否有下一页
         */
        private Boolean hasNext;

        /**
         * 是否有上一页
         */
        private Boolean hasPrev;

        /**
         * 下一页偏移量
         */
        private Integer nextOffset;

        /**
         * 上一页偏移量
         */
        private Integer prevOffset;

        /**
         * 响应时间（毫秒）
         */
        private Long responseTime;

        /**
         * 数据来源
         */
        private String source;

        /**
         * 缓存状态
         */
        private String cacheStatus;

        /**
         * 排序字段
         */
        private String sortBy;

        /**
         * 排序方向
         */
        private String sortOrder;

        /**
         * 过滤条件
         */
        private String filters;

        /**
         * 搜索关键词
         */
        private String keyword;

        /**
         * 搜索结果相关度
         */
        private Double relevance;

        /**
         * 额外的分页信息
         */
        private Object extra;
    }

    /**
     * 检查响应是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return errcode != null && errcode == 0;
    }

    /**
     * 获取错误信息
     * 
     * @return 错误信息
     */
    public String getErrorMessage() {
        if (isSuccess()) {
            return null;
        }
        return errmsg != null ? errmsg : "Unknown error";
    }

    /**
     * 检查是否有数据
     * 
     * @return 是否有数据
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * 检查是否有更多数据（分页）
     * 
     * @return 是否有更多数据
     */
    public boolean hasMoreData() {
        return meta != null && meta.getHasNext() != null && meta.getHasNext();
    }

    /**
     * 获取总记录数
     * 
     * @return 总记录数
     */
    public Integer getTotalCount() {
        return meta != null ? meta.getTotal() : null;
    }

    /**
     * 获取当前偏移量
     * 
     * @return 当前偏移量
     */
    public Integer getCurrentOffset() {
        return meta != null ? meta.getOffset() : null;
    }

    /**
     * 获取下一页偏移量
     * 
     * @return 下一页偏移量
     */
    public Integer getNextOffset() {
        return meta != null ? meta.getNextOffset() : null;
    }
}
