package ec.edu.scli.contracts;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compara contratos OpenAPI con mappings que Spring ya resolvio en runtime.
 * No lee fuentes Java ni comparte codigo con el extractor Python de snapshots.
 */
public final class RuntimeContractVerifier {
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE");

    private RuntimeContractVerifier() {
    }

    public record Operation(String method, String path) implements Comparable<Operation> {
        public Operation {
            method = method.toUpperCase(Locale.ROOT);
            path = normalizePath(path);
            if (!HTTP_METHODS.contains(method)) {
                throw new IllegalArgumentException("Metodo HTTP no soportado: " + method);
            }
        }

        @Override
        public int compareTo(Operation other) {
            int pathComparison = path.compareTo(other.path);
            return pathComparison != 0 ? pathComparison : method.compareTo(other.method);
        }
    }

    public static List<Operation> runtimeOperations(
            RequestMappingHandlerMapping mappings, String applicationPackage) {
        List<Operation> operations = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.getHandlerMethods().entrySet()) {
            Class<?> beanType = entry.getValue().getBeanType();
            if (!beanType.getPackageName().startsWith(applicationPackage)
                    || !AnnotatedElementUtils.hasAnnotation(beanType, RestController.class)) {
                continue;
            }
            RequestMappingInfo info = entry.getKey();
            Set<String> paths = info.getPatternValues();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (paths.isEmpty()) {
                paths = Set.of("/");
            }
            if (methods.isEmpty()) {
                throw new AssertionError("UNBOUNDED_HTTP_METHOD " + beanType.getName()
                        + "#" + entry.getValue().getMethod().getName());
            }
            for (String path : paths) {
                for (RequestMethod method : methods) {
                    operations.add(new Operation(method.name(), path));
                }
            }
        }
        return operations;
    }

    public static List<Operation> openApiOperations(Path contract) throws IOException {
        try {
            JsonFactory factory = JsonFactory.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .build();
            ObjectMapper mapper = new ObjectMapper(factory);
            return openApiOperations(mapper.readTree(contract.toFile()));
        } catch (IOException exception) {
            String message = exception.getMessage() == null ? "JSON invalido" : exception.getMessage();
            if (message.toLowerCase(Locale.ROOT).contains("duplicate")) {
                throw new AssertionError("DUPLICATE_OPERATION " + message, exception);
            }
            throw new IOException("OpenAPI JSON invalido: " + contract + ": " + message, exception);
        }
    }

    public static List<Operation> openApiOperations(JsonNode document) {
        List<Operation> operations = new ArrayList<>();
        JsonNode paths = document.path("paths");
        paths.fields().forEachRemaining(pathEntry -> {
            String path = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();
            pathItem.fieldNames().forEachRemaining(key -> {
                String method = key.toUpperCase(Locale.ROOT);
                if (HTTP_METHODS.contains(method)) {
                    operations.add(new Operation(method, path));
                }
            });
        });
        return operations;
    }

    public static void assertMatches(Collection<Operation> runtime, Collection<Operation> contract) {
        List<String> problems = compare(runtime, contract);
        if (!problems.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), problems));
        }
    }

    public static List<String> compare(Collection<Operation> runtime, Collection<Operation> contract) {
        List<String> problems = new ArrayList<>();
        reportDuplicates("RUNTIME", runtime, problems);
        reportDuplicates("CONTRACT", contract, problems);

        Set<Operation> runtimeSet = new TreeSet<>(runtime);
        Set<Operation> contractSet = new TreeSet<>(contract);
        Set<Operation> missing = new TreeSet<>(runtimeSet);
        missing.removeAll(contractSet);
        Set<Operation> extra = new TreeSet<>(contractSet);
        extra.removeAll(runtimeSet);

        for (Operation operation : missing) {
            problems.add("MISSING_IN_CONTRACT " + operation);
        }
        for (Operation operation : extra) {
            problems.add("EXTRA_IN_CONTRACT " + operation);
        }

        for (Operation left : missing) {
            for (Operation right : extra) {
                if (left.path().equals(right.path()) && !left.method().equals(right.method())) {
                    problems.add("METHOD_MISMATCH runtime=" + left + " contract=" + right);
                } else if (left.method().equals(right.method()) && !left.path().equals(right.path())) {
                    problems.add("PATH_MISMATCH runtime=" + left + " contract=" + right);
                }
            }
        }
        return problems;
    }

    public static Path repositoryFile(String relativePath) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("No se encontro desde user.dir: " + relativePath);
    }

    public static String normalizePath(String rawPath) {
        String path = rawPath == null || rawPath.isBlank() ? "/" : rawPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        StringBuilder normalized = new StringBuilder();
        boolean insideVariable = false;
        boolean previousSlash = false;
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            if (insideVariable) {
                if (character == '}') {
                    normalized.append('}');
                    insideVariable = false;
                }
                continue;
            }
            if (character == '{') {
                normalized.append('{');
                insideVariable = true;
                previousSlash = false;
            } else if (character == '/') {
                if (!previousSlash) {
                    normalized.append(character);
                }
                previousSlash = true;
            } else {
                normalized.append(character);
                previousSlash = false;
            }
        }
        if (insideVariable) {
            throw new IllegalArgumentException("Variable de path sin cierre: " + rawPath);
        }
        while (normalized.length() > 1 && normalized.charAt(normalized.length() - 1) == '/') {
            normalized.deleteCharAt(normalized.length() - 1);
        }
        return normalized.toString();
    }

    private static void reportDuplicates(
            String source, Collection<Operation> operations, List<String> problems) {
        Map<Operation, Integer> counts = new HashMap<>();
        for (Operation operation : operations) {
            counts.merge(operation, 1, Integer::sum);
        }
        Set<Operation> duplicates = new TreeSet<>();
        for (Map.Entry<Operation, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }
        for (Operation duplicate : duplicates) {
            problems.add("DUPLICATE_OPERATION source=" + source + " operation=" + duplicate
                    + " count=" + counts.get(duplicate));
        }
    }
}
