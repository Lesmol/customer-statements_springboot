package com.lvmp.customerstatements_springboot.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadStatementRequest {
    @NotNull
    private MultipartFile file;
}
