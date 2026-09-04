package com.eventplatform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
