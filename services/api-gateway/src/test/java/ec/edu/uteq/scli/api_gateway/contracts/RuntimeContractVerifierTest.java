package ec.edu.uteq.scli.api_gateway.contracts;

import ec.edu.scli.contracts.RuntimeContractVerifier;
import ec.edu.scli.contracts.RuntimeContractVerifier.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(RuntimeContractVerifierTest.MappingFixtureController.class)
@Import(RuntimeContractVerifierTest.MappingFixtureController.class)
class RuntimeContractVerifierTest {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void springExpandePrefijosPathsYMetodosMultiples() {
        List<Operation> operations = RuntimeContractVerifier.runtimeOperations(
                mappings, getClass().getPackageName());

        assertThat(operations).containsExactlyInAnyOrder(
                new Operation("GET", "/fixture/one"),
                new Operation("GET", "/fixture/two"),
                new Operation("GET", "/alternate/one"),
                new Operation("GET", "/alternate/two"),
                new Operation("GET", "/fixture/generic"),
                new Operation("POST", "/fixture/generic"),
                new Operation("GET", "/alternate/generic"),
                new Operation("POST", "/alternate/generic"));
    }

    @Test
    void quitarRutaProduceMissingInContract() {
        assertProblem(List.of(op("GET", "/a")), List.of(), "MISSING_IN_CONTRACT");
    }

    @Test
    void agregarRutaProduceExtraInContract() {
        assertProblem(List.of(), List.of(op("GET", "/inventada")), "EXTRA_IN_CONTRACT");
    }

    @Test
    void cambiarMetodoProduceMethodMismatch() {
        assertProblem(List.of(op("GET", "/a")), List.of(op("POST", "/a")), "METHOD_MISMATCH");
    }

    @Test
    void cambiarPathProducePathMismatch() {
        assertProblem(List.of(op("GET", "/a")), List.of(op("GET", "/b")), "PATH_MISMATCH");
    }

    @Test
    void duplicadoExactoProduceDuplicateOperation() {
        assertProblem(List.of(op("GET", "/a")),
                List.of(op("GET", "/a"), op("GET", "/a")), "DUPLICATE_OPERATION");
    }

    @Test
    void duplicadoSemanticoDeVariableProduceDuplicateOperation() {
        assertProblem(List.of(op("GET", "/a/{id}")),
                List.of(op("GET", "/a/{id}"), op("GET", "/a/{uuid}")),
                "DUPLICATE_OPERATION");
    }

    private static Operation op(String method, String path) {
        return new Operation(method, path);
    }

    private static void assertProblem(
            List<Operation> runtime, List<Operation> contract, String expected) {
        assertThat(RuntimeContractVerifier.compare(runtime, contract))
                .anyMatch(problem -> problem.startsWith(expected));
    }

    @RestController
    @RequestMapping({"/fixture", "/alternate"})
    public static class MappingFixtureController {
        @GetMapping({"/one", "/two"})
        void shortcuts() {
        }

        @RequestMapping(path = "/generic", method = {RequestMethod.GET, RequestMethod.POST})
        void genericMapping() {
        }
    }
}
