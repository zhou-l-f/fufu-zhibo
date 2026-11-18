package com.zlf.fufuzhibo.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页结果
 */
@Data
@Builder
@Schema(name = "PageResult", description = "分页查询结果")
public class PageResult<T> implements Serializable {

    @Schema(description = "当前页码，从 1 开始")
    private long page;

    @Schema(description = "每页条数")
    private long size;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private long pages;

    @Schema(description = "当前页数据")
    private List<T> records;
}
