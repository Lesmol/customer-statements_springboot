package com.lvmp.customerstatements_springboot.model.request;

import com.lvmp.customerstatements_springboot.validation.ValidStatementFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadStatementRequest {
    @ValidStatementFile
    private MultipartFile file;
}
