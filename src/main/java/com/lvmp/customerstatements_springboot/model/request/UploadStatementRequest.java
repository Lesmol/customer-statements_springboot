package com.lvmp.customerstatements_springboot.model.request;

import com.lvmp.customerstatements_springboot.validation.ValidStatementFile;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadStatementRequest {
    @ValidStatementFile
    private MultipartFile file;
}
