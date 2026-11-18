package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(name = "StartLiveDTO", description = "申请开始直播DTO")
public class StartLiveDTO {

    @NotBlank(message = "直播标题不能为空")
    @Size(max = 128, message = "直播标题不能超过 128 个字符")
    private String title;

    @Schema(description = "直播封面图片，不传则本轮直播无封面", type = "string", format = "binary")
    private MultipartFile cover;
}
