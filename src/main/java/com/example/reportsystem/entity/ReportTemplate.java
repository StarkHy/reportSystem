package com.example.reportsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_template")
public class ReportTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private String filePath;

    private String apiUrl;

    private String groovyScript;

    private Integer status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
