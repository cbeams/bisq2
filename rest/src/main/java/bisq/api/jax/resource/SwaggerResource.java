package bisq.api.jax.resource;

import bisq.api.jax.RestApplication;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("openapi.json")
@Produces(MediaType.APPLICATION_JSON)
@Hidden
public class SwaggerResource {
    private static String swaggerJson;

    @GET
    public String swagIt(@Context Application app) {
        if (swaggerJson == null) {
            try {
                OpenAPI api = new OpenAPI();
                Info info = new Info()
                        .title("Bisq v2 REST API")
                        .description("This is the rest API description for Bisq2, For more Information about Bisq, see https://bisq.network")
//                        .termsOfService("http://swagger.io/terms/")
//                        .contact(new Contact()
//                                .email("apiteam@swagger.io"))
                        .license(new License()
                                .name("GNU Affero General Public License")
                                .url("https://github.com/bisq-network/bisq2/blob/main/LICENSE"));

                api.info(info).addServersItem(new Server().url(RestApplication.BASE_URL));
                SwaggerConfiguration configuration = new SwaggerConfiguration().openAPI(api);
                Reader reader = new Reader(configuration);
                OpenAPI openAPI = reader.read(app.getClasses());
                swaggerJson = Json.pretty(openAPI);
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }

        return swaggerJson;
    }
}
