package cn.zest.sso.server.controller.admin;



import cn.zest.sso.common.api.ApiResponse;

import cn.zest.sso.common.api.PageResult;

import cn.zest.sso.server.domain.dto.CreateClientRequest;

import cn.zest.sso.server.domain.dto.UpdateClientRequest;

import cn.zest.sso.server.domain.vo.ClientVO;

import cn.zest.sso.server.domain.vo.CreateClientResultVO;

import cn.zest.sso.server.service.OAuthClientService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

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

@RequestMapping("/api/admin/clients")

@RequiredArgsConstructor

@PreAuthorize("hasRole('SSO_ADMIN')")

public class AdminClientController {



    private final OAuthClientService clientService;



    @GetMapping

    public ApiResponse<PageResult<ClientVO>> listClients(

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "20") int size) {

        Page<ClientVO> result = clientService.pageClients(page, size);

        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));

    }



    @GetMapping("/{clientId}")

    public ApiResponse<ClientVO> getClient(@PathVariable String clientId) {

        return ApiResponse.success(clientService.getByClientId(clientId));

    }



    @PostMapping

    public ApiResponse<CreateClientResultVO> createClient(@Valid @RequestBody CreateClientRequest request) {

        return ApiResponse.success(clientService.createClient(request));

    }



    @PutMapping("/{clientId}")

    public ApiResponse<ClientVO> updateClient(@PathVariable String clientId,

                                              @RequestBody UpdateClientRequest request) {

        return ApiResponse.success(clientService.updateClient(clientId, request));

    }



    @PostMapping("/{clientId}/enable")

    public ApiResponse<Void> enableClient(@PathVariable String clientId) {

        clientService.enableClient(clientId);

        return ApiResponse.success();

    }



    @PostMapping("/{clientId}/disable")

    public ApiResponse<Void> disableClient(@PathVariable String clientId) {

        clientService.disableClient(clientId);

        return ApiResponse.success();

    }



    @PostMapping("/{clientId}/reset-secret")

    public ApiResponse<CreateClientResultVO> resetSecret(@PathVariable String clientId) {

        return ApiResponse.success(clientService.resetClientSecret(clientId));

    }



    @DeleteMapping("/{clientId}")

    public ApiResponse<Void> deleteClient(@PathVariable String clientId) {

        clientService.deleteClient(clientId);

        return ApiResponse.success();

    }

}

