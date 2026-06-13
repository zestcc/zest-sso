package cn.zest.sso.server.controller.admin;



import cn.zest.sso.common.api.ApiResponse;

import cn.zest.sso.common.api.PageResult;

import cn.zest.sso.server.domain.dto.CreateUserRequest;

import cn.zest.sso.server.domain.dto.ResetPasswordRequest;

import cn.zest.sso.server.domain.dto.UpdateUserRequest;

import cn.zest.sso.server.domain.vo.UserInfoVO;

import cn.zest.sso.server.security.SsoUserDetails;

import cn.zest.sso.server.service.MfaService;
import cn.zest.sso.server.service.UserService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/admin/users")

@RequiredArgsConstructor

@PreAuthorize("hasAnyRole('SSO_ADMIN', 'SSO_OPERATOR')")

public class AdminUserController {



    private final UserService userService;

    private final MfaService mfaService;



    @GetMapping

    public ApiResponse<PageResult<UserInfoVO>> listUsers(

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) String keyword) {

        Page<UserInfoVO> result = userService.pageUsers(page, size, keyword);

        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));

    }



    @PostMapping

    public ApiResponse<UserInfoVO> createUser(@Valid @RequestBody CreateUserRequest request) {

        return ApiResponse.success(userService.createUser(request));

    }



    @GetMapping("/me")

    public ApiResponse<UserInfoVO> currentUser(@AuthenticationPrincipal SsoUserDetails userDetails) {

        return ApiResponse.success(userService.getUserInfo(userDetails.getUserId()));

    }



    @GetMapping("/{id}")

    public ApiResponse<UserInfoVO> getUser(@PathVariable Long id) {

        return ApiResponse.success(userService.getUserInfo(id));

    }



    @PutMapping("/{id}")

    public ApiResponse<UserInfoVO> updateUser(@PathVariable Long id,

                                              @RequestBody UpdateUserRequest request) {

        return ApiResponse.success(userService.updateUser(id, request));

    }



    @PostMapping("/{id}/disable")

    public ApiResponse<Void> disableUser(@PathVariable Long id) {

        userService.disableUser(id);

        return ApiResponse.success();

    }



    @PostMapping("/{id}/enable")

    public ApiResponse<Void> enableUser(@PathVariable Long id) {

        userService.enableUser(id);

        return ApiResponse.success();

    }



    @PostMapping("/{id}/unlock")

    public ApiResponse<Void> unlockUser(@PathVariable Long id) {

        userService.unlockUser(id);

        return ApiResponse.success();

    }



    @DeleteMapping("/{id}")

    public ApiResponse<Void> deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);

        return ApiResponse.success();

    }



    @PostMapping("/{id}/reset-password")

    public ApiResponse<Void> resetPassword(@PathVariable Long id,

                                           @Valid @RequestBody ResetPasswordRequest request) {

        userService.resetPassword(id, request.getNewPassword());

        return ApiResponse.success();

    }

    @PostMapping("/{id}/reset-mfa")

    @PreAuthorize("hasRole('SSO_ADMIN')")

    public ApiResponse<Void> resetMfa(@PathVariable Long id) {

        mfaService.adminReset(id);

        return ApiResponse.success();

    }

}

