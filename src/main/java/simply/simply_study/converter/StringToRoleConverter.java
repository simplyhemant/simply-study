package simply.simply_study.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import simply.simply_study.model.enums.Role;

@Component
public class StringToRoleConverter implements Converter<String, Role> {
    @Override
    public Role convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return Role.valueOf(source.trim().toUpperCase());
    }
}
