package ec.edu.scli.usuarios;

import ec.edu.scli.contracts.RuntimeContractVerifier;
import ec.edu.scli.contracts.RuntimeContractVerifier.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuarios_contract;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.initial-data.enabled=false",
        "management.tracing.enabled=false"
})
class OpenApiRuntimeCompletenessTest {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void contratoCoincideConMappingsProcesadosPorSpring() throws Exception {
        List<Operation> runtime = RuntimeContractVerifier.runtimeOperations(
                mappings, "ec.edu.scli.usuarios");
        List<Operation> contract = RuntimeContractVerifier.openApiOperations(
                RuntimeContractVerifier.repositoryFile("docs/openapi/usuarios-service-openapi.json"));

        assertThat(runtime).hasSize(44);
        assertThat(contract).hasSize(44);
        RuntimeContractVerifier.assertMatches(runtime, contract);
    }
}
