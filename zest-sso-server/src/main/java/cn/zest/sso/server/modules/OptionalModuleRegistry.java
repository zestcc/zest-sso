package cn.zest.sso.server.modules;

import cn.zest.sso.server.modules.spi.OptionalModule;
import cn.zest.sso.server.modules.spi.OptionalModuleDescriptor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class OptionalModuleRegistry {

    private final List<OptionalModule> modules;

    public OptionalModuleRegistry(List<OptionalModule> modules) {
        this.modules = modules;
    }

    public List<OptionalModuleDescriptor> listDescriptors() {
        return modules.stream()
                .map(OptionalModule::descriptor)
                .sorted(Comparator.comparing(OptionalModuleDescriptor::category)
                        .thenComparing(OptionalModuleDescriptor::key))
                .toList();
    }

    public boolean isEnabled(String moduleKey) {
        return modules.stream()
                .filter(m -> m.moduleKey().equals(moduleKey))
                .findFirst()
                .map(OptionalModule::isEnabled)
                .orElse(false);
    }
}
