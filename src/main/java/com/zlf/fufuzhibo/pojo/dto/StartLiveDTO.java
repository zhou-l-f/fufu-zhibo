package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "StartLiveDTO", description = "申请开始直播DTO")
public class StartLiveDTO {

    @NotBlank(message = "直播标题不能为空")
    @Size(max = 128, message = "直播标题不能超过 128 个字符")
    private String title;

    @Size(max = 512, message = "封面地址不能超过 512 个字符")
    private String coverUrl;
}
