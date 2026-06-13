package cn.zest.sso.server.scim;

import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.server.domain.entity.SsoGroup;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.entity.SsoUserGroup;
import cn.zest.sso.server.domain.mapper.SsoUserGroupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ScimPatchOperations {

    private ScimPatchOperations() {
    }

    static void applyUserOperation(SsoUser user, Map<?, ?> operation) {
        String op = String.valueOf(operation.get("op"));
        String path = operation.get("path") != null ? String.valueOf(operation.get("path")) : null;
        Object value = operation.get("value");

        if ("remove".equalsIgnoreCase(op)) {
            applyUserRemove(user, path);
            return;
        }
        if ("add".equalsIgnoreCase(op)) {
            applyUserAdd(user, path, value);
            return;
        }
        if ("replace".equalsIgnoreCase(op)) {
            if (StringUtils.hasText(path)) {
                applyUserPathReplace(user, path, value);
            } else if (value instanceof Map<?, ?> values) {
                applyUserMapReplace(user, values);
            } else {
                applyUserPathReplace(user, inferPathFromValue(value), value);
            }
        }
    }

    static void applyGroupOperation(SsoGroup group, Long groupId, SsoUserGroupMapper userGroupMapper,
                                    Map<?, ?> operation) {
        String op = String.valueOf(operation.get("op"));
        String path = operation.get("path") != null ? String.valueOf(operation.get("path")) : null;
        Object value = operation.get("value");

        if ("remove".equalsIgnoreCase(op)) {
            if (path != null && path.startsWith("members")) {
                removeGroupMembers(groupId, userGroupMapper, value, path);
            }
            return;
        }
        if ("add".equalsIgnoreCase(op) && path != null && path.startsWith("members")) {
            addGroupMembers(groupId, userGroupMapper, value);
            return;
        }
        if ("replace".equalsIgnoreCase(op)) {
            if ("displayName".equalsIgnoreCase(path)) {
                if (value != null) {
                    group.setName(String.valueOf(value));
                }
            } else if (!StringUtils.hasText(path) && value instanceof Map<?, ?> values && values.containsKey("displayName")) {
                group.setName(String.valueOf(values.get("displayName")));
            } else if (path != null && path.startsWith("members")) {
                userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>().eq(SsoUserGroup::getGroupId, groupId));
                addGroupMembers(groupId, userGroupMapper, value);
            } else if (value instanceof Map<?, ?> values && values.containsKey("members")) {
                userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>().eq(SsoUserGroup::getGroupId, groupId));
                addGroupMembers(groupId, userGroupMapper, values.get("members"));
            }
        }
    }

    private static void applyUserRemove(SsoUser user, String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        if ("externalId".equalsIgnoreCase(path)) {
            user.setExternalId(null);
        } else if (path.startsWith("emails")) {
            user.setEmail(null);
        }
    }

    private static void applyUserAdd(SsoUser user, String path, Object value) {
        if (path != null && path.startsWith("emails")) {
            String email = extractEmailValue(value);
            if (email != null) {
                user.setEmail(email);
            }
        } else if (value instanceof Map<?, ?> values) {
            applyUserMapReplace(user, values);
        }
    }

    private static void applyUserPathReplace(SsoUser user, String path, Object value) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        switch (path) {
            case "active" -> user.setStatus(Boolean.parseBoolean(String.valueOf(value))
                    ? UserStatus.ACTIVE.getCode() : UserStatus.DISABLED.getCode());
            case "userName" -> user.setUsername(String.valueOf(value));
            case "displayName" -> user.setDisplayName(String.valueOf(value));
            case "externalId" -> user.setExternalId(String.valueOf(value));
            case "name.formatted" -> user.setDisplayName(String.valueOf(value));
            default -> {
                if (path.startsWith("emails")) {
                    String email = extractEmailValue(value);
                    if (email != null) {
                        user.setEmail(email);
                    }
                }
            }
        }
    }

    private static void applyUserMapReplace(SsoUser user, Map<?, ?> values) {
        if (values.containsKey("active")) {
            user.setStatus(Boolean.TRUE.equals(values.get("active"))
                    ? UserStatus.ACTIVE.getCode() : UserStatus.DISABLED.getCode());
        }
        if (values.containsKey("userName")) {
            user.setUsername(String.valueOf(values.get("userName")));
        }
        if (values.containsKey("displayName")) {
            user.setDisplayName(String.valueOf(values.get("displayName")));
        }
        if (values.containsKey("externalId")) {
            user.setExternalId(String.valueOf(values.get("externalId")));
        }
        if (values.containsKey("emails")) {
            String email = extractEmailValue(values.get("emails"));
            if (email != null) {
                user.setEmail(email);
            }
        }
        if (values.containsKey("name") && values.get("name") instanceof Map<?, ?> name) {
            if (name.containsKey("formatted")) {
                user.setDisplayName(String.valueOf(name.get("formatted")));
            }
        }
    }

    private static String inferPathFromValue(Object value) {
        if (value instanceof Map<?, ?> values) {
            if (values.containsKey("active")) {
                return "active";
            }
            if (values.containsKey("userName")) {
                return "userName";
            }
        }
        return null;
    }

    private static String extractEmailValue(Object value) {
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof Map<?, ?> map && map.get("value") != null) {
            return String.valueOf(map.get("value"));
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            return extractEmailValue(list.get(0));
        }
        return null;
    }

    private static void addGroupMembers(Long groupId, SsoUserGroupMapper userGroupMapper, Object value) {
        for (ScimMember member : toMembers(value)) {
            if (!StringUtils.hasText(member.getValue())) {
                continue;
            }
            Long userId = Long.parseLong(member.getValue());
            Long count = userGroupMapper.selectCount(new LambdaQueryWrapper<SsoUserGroup>()
                    .eq(SsoUserGroup::getGroupId, groupId)
                    .eq(SsoUserGroup::getUserId, userId));
            if (count == 0) {
                SsoUserGroup ug = new SsoUserGroup();
                ug.setUserId(userId);
                ug.setGroupId(groupId);
                userGroupMapper.insert(ug);
            }
        }
    }

    private static void removeGroupMembers(Long groupId, SsoUserGroupMapper userGroupMapper, Object value, String path) {
        if (value == null && path.contains("value eq")) {
            String memberId = path.replaceAll(".*value eq \"([^\"]+)\".*", "$1");
            userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>()
                    .eq(SsoUserGroup::getGroupId, groupId)
                    .eq(SsoUserGroup::getUserId, Long.parseLong(memberId)));
            return;
        }
        for (ScimMember member : toMembers(value)) {
            if (StringUtils.hasText(member.getValue())) {
                userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>()
                        .eq(SsoUserGroup::getGroupId, groupId)
                        .eq(SsoUserGroup::getUserId, Long.parseLong(member.getValue())));
            }
        }
    }

    private static List<ScimMember> toMembers(Object value) {
        List<ScimMember> members = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    members.add(ScimMember.builder()
                            .value(map.get("value") != null ? String.valueOf(map.get("value")) : null)
                            .display(map.get("display") != null ? String.valueOf(map.get("display")) : null)
                            .type(map.get("type") != null ? String.valueOf(map.get("type")) : "User")
                            .build());
                }
            }
        } else if (value instanceof Map<?, ?> map) {
            members.add(ScimMember.builder()
                    .value(map.get("value") != null ? String.valueOf(map.get("value")) : null)
                    .build());
        }
        return members;
    }
}
