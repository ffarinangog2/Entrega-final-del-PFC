package ec.edu.scli.academico;

import ec.edu.scli.contracts.RuntimeContractVerifier;
import ec.edu.scli.contracts.RuntimeContractVerifier.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.initial-data.enabled=false",
        "management.tracing.enabled=false"
})
@ActiveProfiles("test")
class OpenApiRuntimeCompletenessTest {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void contratoCoincideConMappingsProcesadosPorSpring() throws Exception {
        List<Operation> runtime = RuntimeContractVerifier.runtimeOperations(
                mappings, "ec.edu.scli.academico");
        List<Operation> contract = RuntimeContractVerifier.openApiOperations(
                RuntimeContractVerifier.repositoryFile(
                        "docs/openapi/academico-laboratorios-service-openapi.json"));

        assertThat(runtime).hasSize(71);
        assertThat(contract).hasSize(71);
        RuntimeContractVerifier.assertMatches(runtime, contract);
    }
}
