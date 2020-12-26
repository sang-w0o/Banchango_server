package com.banchango.users.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@NoArgsConstructor
@Getter
@Setter
public class UserSigninRequestDto {

    @NotBlank(message = "Email is required.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;
}
