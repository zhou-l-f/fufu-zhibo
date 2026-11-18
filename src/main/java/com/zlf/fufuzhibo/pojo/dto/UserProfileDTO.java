package com.zlf.fufuzhibo.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 完善个人信息 DTO
 */
@Data
@Schema(name = "UserProfileDTO", description = "完善个人信息DTO")
public class UserProfileDTO {

    @Size(max = 64, message = "昵称长度不能超过64位")
    @Schema(description = "昵称，不传则不修改")
    private String name;

    @Min(value = 0, message = "性别取值不正确")
    @Max(value = 2, message = "性别取值不正确")
    @Schema(description = "性别：0 未知、1 男、2 女，不传则不修改")
    private Integer gender;

    @Schema(description = "头像文件，不传则不修改", type = "string", format = "binary")
    private MultipartFile avatar;
}
