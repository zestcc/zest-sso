package cn.zest.sso.server.scim;

import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoGroup;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.entity.SsoUserGroup;
import cn.zest.sso.server.domain.mapper.SsoGroupMapper;
import cn.zest.sso.server.domain.mapper.SsoUserGroupMapper;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.service.PasswordPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScimService {

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    private static final String GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
    private static final String LIST_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";
    private static final String SPC_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";
    private static final String BULK_REQUEST_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkRequest";
    private static final String BULK_RESPONSE_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkResponse";
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+) eq \"([^\"]+)\"");
    private static final int MAX_BULK_OPERATIONS = 100;

    private final SsoUserMapper userMapper;
    private final SsoGroupMapper groupMapper;
    private final SsoUserGroupMapper userGroupMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SsoProperties ssoProperties;

    public ScimListResponse<ScimUserResource> listUsers(String filter, Integer startIndex, Integer count) {
        int page = startIndex != null && startIndex > 0 ? startIndex : 1;
        int size = count != null && count > 0 ? count : 20;
        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        applyUserFilter(wrapper, filter);
        Page<SsoUser> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        return ScimListResponse.<ScimUserResource>builder()
                .schemas(List.of(LIST_SCHEMA))
                .totalResults((int) userPage.getTotal())
                .startIndex(page)
                .itemsPerPage(size)
                .Resources(userPage.getRecords().stream().map(this::toUserResource).toList())
                .build();
    }

    public ScimUserResource getUser(String id) {
        return toUserResource(findUser(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScimUserResource createUser(ScimUserResource resource) {
        if (!StringUtils.hasText(resource.getUserName())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "userName 不能为空");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getUsername, resource.getUserName()));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "用户名已存在");
        }
        SsoUser user = new SsoUser();
        user.setUsername(resource.getUserName());
        user.setEmail(resolveEmail(resource));
        user.setDisplayName(resolveDisplayName(resource));
        String password = UUID.randomUUID().toString().replace("-", "") + "Aa1!";
        passwordPolicyService.validateNewPassword(null, password);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(Boolean.FALSE.equals(resource.getActive()) ? UserStatus.DISABLED.getCode() : UserStatus.ACTIVE.getCode());
        user.setIsSuperAdmin(0);
        user.setMfaEnabled(0);
        user.setExternalId(resource.getExternalId());
        user.setFederationSource("SCIM");
        user.setPasswordChangedAt(LocalDateTime.now());
        userMapper.insert(user);
        passwordPolicyService.recordPasswordChange(user.getId(), user.getPasswordHash());
        return toUserResource(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScimUserResource replaceUser(String id, ScimUserResource resource) {
        SsoUser user = findUser(id);
        if (StringUtils.hasText(resource.getUserName())) {
            user.setUsername(resource.getUserName());
        }
        if (resource.getEmails() != null && !resource.getEmails().isEmpty()) {
            user.setEmail(resource.getEmails().get(0).getValue());
        }
        if (resource.getDisplayName() != null) {
            user.setDisplayName(resource.getDisplayName());
        } else if (resource.getName() != null && resource.getName().getFormatted() != null) {
            user.setDisplayName(resource.getName().getFormatted());
        }
        if (resource.getActive() != null) {
            user.setStatus(resource.getActive() ? UserStatus.ACTIVE.getCode() : UserStatus.DISABLED.getCode());
        }
        if (resource.getExternalId() != null) {
            user.setExternalId(resource.getExternalId());
        }
        userMapper.updateById(user);
        return toUserResource(userMapper.selectById(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScimUserResource patchUser(String id, Map<String, Object> patchBody) {
        SsoUser user = findUser(id);
        Object operations = patchBody.get("Operations");
        if (operations instanceof List<?> ops) {
            for (Object op : ops) {
                if (op instanceof Map<?, ?> operation) {
                    ScimPatchOperations.applyUserOperation(user, operation);
                }
            }
        }
        userMapper.updateById(user);
        return toUserResource(userMapper.selectById(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String id) {
        userMapper.deleteById(findUser(id).getId());
    }

    public ScimListResponse<ScimGroupResource> listGroups(String filter, Integer startIndex, Integer count) {
        int page = startIndex != null && startIndex > 0 ? startIndex : 1;
        int size = count != null && count > 0 ? count : 20;
        LambdaQueryWrapper<SsoGroup> wrapper = new LambdaQueryWrapper<>();
        applyGroupFilter(wrapper, filter);
        Page<SsoGroup> groupPage = groupMapper.selectPage(new Page<>(page, size), wrapper);
        return ScimListResponse.<ScimGroupResource>builder()
                .schemas(List.of(LIST_SCHEMA))
                .totalResults((int) groupPage.getTotal())
                .startIndex(page)
                .itemsPerPage(size)
                .Resources(groupPage.getRecords().stream().map(this::toGroupResource).toList())
                .build();
    }

    public ScimGroupResource getGroup(String id) {
        return toGroupResource(findGroup(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScimGroupResource createGroup(ScimGroupResource resource) {
        if (!StringUtils.hasText(resource.getDisplayName())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "displayName 不能为空");
        }
        String code = toGroupCode(resource.getDisplayName());
        Long count = groupMapper.selectCount(new LambdaQueryWrapper<SsoGroup>().eq(SsoGroup::getCode, code));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "用户组已存在");
        }
        SsoGroup group = new SsoGroup();
        group.setCode(code);
        group.setName(resource.getDisplayName());
        groupMapper.insert(group);
        bindGroupMembers(group.getId(), resource.getMembers());
        return toGroupResource(groupMapper.selectById(group.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScimGroupResource replaceGroup(String id, ScimGroupResource resource) {
        SsoGroup group = findGroup(id);
        if (StringUtils.hasText(resource.getDisplayName())) {
            group.setName(resource.getDisplayName());
            groupMapper.updateById(group);
        }
        if (resource.getMembers() != null) {
            userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>().eq(SsoUserGroup::getGroupId, group.getId()));
            bindGroupMembers(group.getId(), resource.getMembers());
        }
        return toGroupResource(groupMapper.selectById(group.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScimGroupResource patchGroup(String id, Map<String, Object> patchBody) {
        SsoGroup group = findGroup(id);
        Object operations = patchBody.get("Operations");
        if (operations instanceof List<?> ops) {
            for (Object op : ops) {
                if (op instanceof Map<?, ?> operation) {
                    ScimPatchOperations.applyGroupOperation(group, group.getId(), userGroupMapper, operation);
                }
            }
        }
        groupMapper.updateById(group);
        return toGroupResource(groupMapper.selectById(group.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(String id) {
        SsoGroup group = findGroup(id);
        userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>().eq(SsoUserGroup::getGroupId, group.getId()));
        groupMapper.deleteById(group.getId());
    }

    public ScimServiceProviderConfig serviceProviderConfig() {
        return ScimServiceProviderConfig.builder()
                .schemas(List.of(SPC_SCHEMA))
                .documentationUri("https://datatracker.ietf.org/doc/html/rfc7644")
                .patch(Map.of("supported", true))
                .bulk(Map.of("supported", true, "maxOperations", MAX_BULK_OPERATIONS, "maxPayloadSize", 1048576))
                .filter(Map.of("supported", true, "maxResults", 200))
                .changePassword(Map.of("supported", false))
                .sort(Map.of("supported", false))
                .etag(Map.of("supported", false))
                .authenticationSchemes(List.of("oauthbearertoken"))
                .build();
    }

    public Map<String, Object> resourceTypes() {
        return Map.of(
                "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                "totalResults", 2,
                "Resources", List.of(
                        Map.of("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                                "id", "User", "name", "User", "endpoint", "/Users", "schema", USER_SCHEMA),
                        Map.of("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                                "id", "Group", "name", "Group", "endpoint", "/Groups", "schema", GROUP_SCHEMA)
                )
        );
    }

    public Map<String, Object> schemas() {
        return Map.of(
                "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                "totalResults", 2,
                "Resources", List.of(
                        Map.of("id", USER_SCHEMA, "name", "User"),
                        Map.of("id", GROUP_SCHEMA, "name", "Group")
                )
        );
    }

    public ScimBulkResponse processBulk(ScimBulkRequest request) {
        if (request.getOperations() == null || request.getOperations().isEmpty()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Bulk Operations 不能为空");
        }
        if (request.getOperations().size() > MAX_BULK_OPERATIONS) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Bulk 操作数超过上限: " + MAX_BULK_OPERATIONS);
        }
        List<ScimBulkOperationResponse> responses = new ArrayList<>();
        int failures = 0;
        for (ScimBulkOperation operation : request.getOperations()) {
            ScimBulkOperationResponse response = executeBulkOperation(operation);
            responses.add(response);
            if (response.getStatus() != null && response.getStatus() >= 400) {
                failures++;
                if (request.getFailOnErrors() != null && failures >= request.getFailOnErrors()) {
                    break;
                }
            }
        }
        return ScimBulkResponse.builder()
                .schemas(List.of(BULK_RESPONSE_SCHEMA))
                .Operations(responses)
                .build();
    }

    private ScimBulkOperationResponse executeBulkOperation(ScimBulkOperation operation) {
        try {
            String method = operation.getMethod() != null ? operation.getMethod().toUpperCase() : "";
            String path = operation.getPath() != null ? operation.getPath() : "";
            return switch (method) {
                case "POST" -> bulkCreate(path, operation);
                case "PUT" -> bulkReplace(path, operation);
                case "PATCH" -> bulkPatch(path, operation);
                case "DELETE" -> bulkDelete(path, operation);
                default -> bulkError(operation, 400, "不支持的 Bulk method: " + method);
            };
        } catch (SsoException e) {
            return bulkError(operation, mapStatus(e.getCode()), e.getMessage());
        } catch (Exception e) {
            return bulkError(operation, 500, e.getMessage());
        }
    }

    private ScimBulkOperationResponse bulkCreate(String path, ScimBulkOperation operation) {
        if ("/Users".equals(path)) {
            ScimUserResource created = createUser(convertData(operation.getData(), ScimUserResource.class));
            return ScimBulkOperationResponse.builder()
                    .bulkId(operation.getBulkId())
                    .method("POST")
                    .status(201)
                    .location(created.getMeta() != null ? created.getMeta().getLocation() : null)
                    .response(created)
                    .build();
        }
        if ("/Groups".equals(path)) {
            ScimGroupResource created = createGroup(convertData(operation.getData(), ScimGroupResource.class));
            return ScimBulkOperationResponse.builder()
                    .bulkId(operation.getBulkId())
                    .method("POST")
                    .status(201)
                    .location(created.getMeta() != null ? created.getMeta().getLocation() : null)
                    .response(created)
                    .build();
        }
        return bulkError(operation, 404, "不支持的 Bulk 路径: " + path);
    }

    private ScimBulkOperationResponse bulkReplace(String path, ScimBulkOperation operation) {
        String id = extractResourceId(path);
        if (path.startsWith("/Users/")) {
            ScimUserResource updated = replaceUser(id, convertData(operation.getData(), ScimUserResource.class));
            return successResponse(operation, "PUT", 200, updated);
        }
        if (path.startsWith("/Groups/")) {
            ScimGroupResource updated = replaceGroup(id, convertData(operation.getData(), ScimGroupResource.class));
            return successResponse(operation, "PUT", 200, updated);
        }
        return bulkError(operation, 404, "不支持的 Bulk 路径: " + path);
    }

    private ScimBulkOperationResponse bulkPatch(String path, ScimBulkOperation operation) {
        String id = extractResourceId(path);
        Map<String, Object> patchBody = convertData(operation.getData(), Map.class);
        if (path.startsWith("/Users/")) {
            ScimUserResource updated = patchUser(id, patchBody);
            return successResponse(operation, "PATCH", 200, updated);
        }
        if (path.startsWith("/Groups/")) {
            ScimGroupResource updated = patchGroup(id, patchBody);
            return successResponse(operation, "PATCH", 200, updated);
        }
        return bulkError(operation, 404, "不支持的 Bulk 路径: " + path);
    }

    private ScimBulkOperationResponse bulkDelete(String path, ScimBulkOperation operation) {
        String id = extractResourceId(path);
        if (path.startsWith("/Users/")) {
            deleteUser(id);
            return ScimBulkOperationResponse.builder()
                    .bulkId(operation.getBulkId())
                    .method("DELETE")
                    .status(204)
                    .build();
        }
        if (path.startsWith("/Groups/")) {
            deleteGroup(id);
            return ScimBulkOperationResponse.builder()
                    .bulkId(operation.getBulkId())
                    .method("DELETE")
                    .status(204)
                    .build();
        }
        return bulkError(operation, 404, "不支持的 Bulk 路径: " + path);
    }

    private ScimBulkOperationResponse successResponse(ScimBulkOperation operation, String method, int status, Object body) {
        return ScimBulkOperationResponse.builder()
                .bulkId(operation.getBulkId())
                .method(method)
                .status(status)
                .response(body)
                .build();
    }

    private ScimBulkOperationResponse bulkError(ScimBulkOperation operation, int status, String detail) {
        return ScimBulkOperationResponse.builder()
                .bulkId(operation.getBulkId())
                .method(operation.getMethod())
                .status(status)
                .response(ScimErrorResponse.builder()
                        .schemas(List.of("urn:ietf:params:scim:api:messages:2.0:Error"))
                        .status(status)
                        .detail(detail)
                        .build())
                .build();
    }

    private String extractResourceId(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    @SuppressWarnings("unchecked")
    private <T> T convertData(Object data, Class<T> type) {
        if (data == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Bulk data 不能为空");
        }
        if (type.isInstance(data)) {
            return (T) data;
        }
        if (data instanceof Map<?, ?> map) {
            if (type == ScimUserResource.class) {
                return (T) mapToUserResource((Map<String, Object>) map);
            }
            if (type == ScimGroupResource.class) {
                return (T) mapToGroupResource((Map<String, Object>) map);
            }
            return (T) map;
        }
        throw new SsoException(ErrorCode.BAD_REQUEST, "无法解析 Bulk data");
    }

    private ScimUserResource mapToUserResource(Map<String, Object> map) {
        ScimUserResource.ScimUserResourceBuilder builder = ScimUserResource.builder()
                .schemas(List.of(USER_SCHEMA))
                .userName((String) map.get("userName"))
                .displayName((String) map.get("displayName"))
                .externalId((String) map.get("externalId"));
        if (map.get("active") != null) {
            builder.active(Boolean.valueOf(String.valueOf(map.get("active"))));
        }
        return builder.build();
    }

    private ScimGroupResource mapToGroupResource(Map<String, Object> map) {
        return ScimGroupResource.builder()
                .schemas(List.of(GROUP_SCHEMA))
                .displayName((String) map.get("displayName"))
                .build();
    }

    private int mapStatus(int code) {
        return switch (code) {
            case 401 -> 401;
            case 403 -> 403;
            case 404 -> 404;
            case 409 -> 409;
            default -> 400;
        };
    }

    private void bindGroupMembers(Long groupId, List<ScimMember> members) {
        if (members == null) {
            return;
        }
        for (ScimMember member : members) {
            if (member == null || !StringUtils.hasText(member.getValue())) {
                continue;
            }
            SsoUser user = userMapper.selectById(Long.parseLong(member.getValue()));
            if (user != null) {
                SsoUserGroup ug = new SsoUserGroup();
                ug.setUserId(user.getId());
                ug.setGroupId(groupId);
                userGroupMapper.insert(ug);
            }
        }
    }

    private void applyUserFilter(LambdaQueryWrapper<SsoUser> wrapper, String filter) {
        if (!StringUtils.hasText(filter)) {
            return;
        }
        Matcher matcher = FILTER_PATTERN.matcher(filter.trim());
        if (!matcher.matches()) {
            return;
        }
        String field = matcher.group(1);
        String value = matcher.group(2);
        if ("userName".equalsIgnoreCase(field)) {
            wrapper.eq(SsoUser::getUsername, value);
        } else if ("externalId".equalsIgnoreCase(field)) {
            wrapper.eq(SsoUser::getExternalId, value);
        }
    }

    private void applyGroupFilter(LambdaQueryWrapper<SsoGroup> wrapper, String filter) {
        if (!StringUtils.hasText(filter)) {
            return;
        }
        Matcher matcher = FILTER_PATTERN.matcher(filter.trim());
        if (!matcher.matches()) {
            return;
        }
        String field = matcher.group(1);
        String value = matcher.group(2);
        if ("displayName".equalsIgnoreCase(field)) {
            wrapper.eq(SsoGroup::getName, value);
        }
    }

    private SsoUser findUser(String id) {
        SsoUser user = userMapper.selectById(Long.parseLong(id));
        if (user == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "SCIM 用户不存在");
        }
        return user;
    }

    private SsoGroup findGroup(String id) {
        SsoGroup group = groupMapper.selectById(Long.parseLong(id));
        if (group == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "SCIM 用户组不存在");
        }
        return group;
    }

    private ScimUserResource toUserResource(SsoUser user) {
        String location = ssoProperties.getIssuer() + "/scim/v2/Users/" + user.getId();
        return ScimUserResource.builder()
                .schemas(List.of(USER_SCHEMA))
                .id(String.valueOf(user.getId()))
                .externalId(user.getExternalId())
                .userName(user.getUsername())
                .displayName(user.getDisplayName())
                .name(ScimName.builder().formatted(user.getDisplayName()).build())
                .emails(StringUtils.hasText(user.getEmail())
                        ? List.of(ScimEmail.builder().value(user.getEmail()).primary(true).build())
                        : List.of())
                .active(user.getStatus() != null && user.getStatus() == UserStatus.ACTIVE.getCode())
                .meta(ScimMeta.builder()
                        .resourceType("User")
                        .location(location)
                        .created(formatTime(user.getCreateTime()))
                        .lastModified(formatTime(user.getUpdateTime()))
                        .build())
                .build();
    }

    private ScimGroupResource toGroupResource(SsoGroup group) {
        List<SsoUserGroup> relations = userGroupMapper.selectList(new LambdaQueryWrapper<SsoUserGroup>()
                .eq(SsoUserGroup::getGroupId, group.getId()));
        List<ScimMember> members = new ArrayList<>();
        for (SsoUserGroup relation : relations) {
            SsoUser user = userMapper.selectById(relation.getUserId());
            if (user != null) {
                members.add(ScimMember.builder()
                        .value(String.valueOf(user.getId()))
                        .display(user.getDisplayName())
                        .type("User")
                        .build());
            }
        }
        String location = ssoProperties.getIssuer() + "/scim/v2/Groups/" + group.getId();
        return ScimGroupResource.builder()
                .schemas(List.of(GROUP_SCHEMA))
                .id(String.valueOf(group.getId()))
                .displayName(group.getName())
                .members(members)
                .meta(ScimMeta.builder()
                        .resourceType("Group")
                        .location(location)
                        .created(formatTime(group.getCreateTime()))
                        .lastModified(formatTime(group.getUpdateTime()))
                        .build())
                .build();
    }

    private String resolveEmail(ScimUserResource resource) {
        if (resource.getEmails() != null && !resource.getEmails().isEmpty()) {
            return resource.getEmails().get(0).getValue();
        }
        return resource.getUserName() + "@scim.local";
    }

    private String resolveDisplayName(ScimUserResource resource) {
        if (StringUtils.hasText(resource.getDisplayName())) {
            return resource.getDisplayName();
        }
        if (resource.getName() != null && StringUtils.hasText(resource.getName().getFormatted())) {
            return resource.getName().getFormatted();
        }
        return resource.getUserName();
    }

    private String toGroupCode(String displayName) {
        return displayName.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
