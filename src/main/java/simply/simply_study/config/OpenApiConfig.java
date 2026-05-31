package simply.simply_study.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simply Study Booking API")
                        .version("1.0.0")
                        .description("API documentation for the Simply Study global class offering and booking system.")
                        .contact(new Contact()
                                .name("Simply Study Support")
                                .email("support@simplystudy.com")));
    }

    @Bean
    public OperationCustomizer addGlobalHeaders() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("UserId")
                    .description("The ID of the requesting user (Teacher or Parent)")
                    .required(false)
                    .schema(new StringSchema()));

            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-User-Id")
                    .description("Alternative header for user ID")
                    .required(false)
                    .schema(new StringSchema()));

            StringSchema timezoneSchema = new StringSchema();
            timezoneSchema.setDefault("UTC");

            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Timezone")
                    .description("The local timezone for formatting session schedules (e.g. Asia/Kolkata, America/New_York)")
                    .required(false)
                    .schema(timezoneSchema));

            return operation;
        };
    }
}
