package cn.zest.sso.server.plugin;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.entity.SsoAlertChannel;
import cn.zest.sso.server.domain.mapper.SsoAlertChannelMapper;
import cn.zest.sso.server.domain.vo.AlertChannelConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertChannelConfigService {

    private final SsoAlertChannelMapper alertChannelMapper;
    private final PluginConfigService pluginConfigService;
    private final ObjectMapper objectMapper;

    public List<AlertChannelConfigVO> listEnabled() {
        return alertChannelMapper.selectList(new LambdaQueryWrapper<SsoAlertChannel>()
                        .eq(SsoAlertChannel::getEnabled, 1)
                        .orderByAsc(SsoAlertChannel::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<AlertChannelConfigVO> listAll() {
        return alertChannelMapper.selectList(new LambdaQueryWrapper<SsoAlertChannel>()
                        .orderByDesc(SsoAlertChannel::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public AlertChannelConfigVO getById(Long id) {
        SsoAlertChannel row = alertChannelMapper.selectById(id);
        if (row == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "告警通道不存在");
        }
        return toVO(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertChannelConfigVO create(AlertChannelConfigVO request) {
        SsoAlertChannel row = new SsoAlertChannel();
        row.setName(request.getName());
        row.setChannelKey(request.getChannelKey());
        row.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        row.setEvents(serializeEvents(request.getEvents()));
        row.setConfig(pluginConfigService.toConfigJson(request.getConfig()));
        alertChannelMapper.insert(row);
        return toVO(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public AlertChannelConfigVO update(Long id, AlertChannelConfigVO request) {
        SsoAlertChannel row = alertChannelMapper.selectById(id);
        if (row == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "告警通道不存在");
        }
        if (request.getName() != null) {
            row.setName(request.getName());
        }
        if (request.getChannelKey() != null) {
            row.setChannelKey(request.getChannelKey());
        }
        if (request.getEnabled() != null) {
            row.setEnabled(request.getEnabled());
        }
        if (request.getEvents() != null) {
            row.setEvents(serializeEvents(request.getEvents()));
        }
        if (request.getConfig() != null) {
            Map<String, String> merged = pluginConfigService.parseConfigJson(row.getConfig());
            merged.putAll(request.getConfig());
            row.setConfig(pluginConfigService.toConfigJson(merged));
        }
        alertChannelMapper.updateById(row);
        return toVO(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        alertChannelMapper.deleteById(id);
    }

    public List<String> parseEvents(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> events = objectMapper.readValue(json, new TypeReference<>() {});
            return events != null ? events : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String serializeEvents(List<String> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(events);
        } catch (Exception e) {
            return null;
        }
    }

    private AlertChannelConfigVO toVO(SsoAlertChannel row) {
        return AlertChannelConfigVO.builder()
                .id(row.getId())
                .name(row.getName())
                .channelKey(row.getChannelKey())
                .enabled(row.getEnabled())
                .events(parseEvents(row.getEvents()))
                .config(pluginConfigService.maskSecrets(pluginConfigService.parseConfigJson(row.getConfig())))
                .build();
    }
}
