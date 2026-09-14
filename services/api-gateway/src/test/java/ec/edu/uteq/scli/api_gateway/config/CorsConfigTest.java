package ec.edu.uteq.scli.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

class CorsConfigTest {

    @Test
    void permiteLosOrigenesLocalesPredeterminados() throws Exception {
        CorsFilter filter = new CorsConfig(
                "http://localhost:3000,http://localhost:5173").corsFilter();

        MockHttpServletResponse response = ejecutarPreflight(filter, "http://localhost:5173");

        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:5173");
    }

    @Test
    void permiteOrigenesConfiguradosSeparadosPorComas() throws Exception {
        CorsFilter filter = new CorsConfig(
                "http://localhost:3000, http://34.45.53.240:3000").corsFilter();

        MockHttpServletResponse response = ejecutarPreflight(filter, "http://34.45.53.240:3000");

        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("http://34.45.53.240:3000");
    }

    @Test
    void rechazaOrigenesNoConfigurados() throws Exception {
        CorsFilter filter = new CorsConfig(
                "http://localhost:3000,http://localhost:5173").corsFilter();

        MockHttpServletResponse response = ejecutarPreflight(filter, "http://example.com");

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    private MockHttpServletResponse ejecutarPreflight(CorsFilter filter, String origin)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/reservas");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });
        return response;
    }
}
