package ec.edu.scli.contract.reservas;
import au.com.dius.pact.consumer.*; import au.com.dius.pact.consumer.dsl.*; import au.com.dius.pact.consumer.junit5.*;
import au.com.dius.pact.core.model.V4Pact; import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith;
import java.net.*; import java.net.http.*; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(PactConsumerTestExt.class) @PactTestFor(providerName="reservas-solicitudes-service")
class MobileIncidentesNotificacionesPactTest {
 static final String ID="55555555-5555-5555-5555-555555555555";
 @Pact(consumer="scli-mobile") V4Pact crear(PactDslWithProvider b){
  var req=new PactDslJsonBody().stringValue("laboratorioEquipo","Laboratorio 1").stringValue("descripcion","Equipo sin red").stringValue("prioridad","ALTA").stringValue("fecha","2026-08-31");
  return b.given("usuario autenticado puede reportar incidentes").uponReceiving("Mobile crea un incidente")
   .path("/api/v1/incidentes").method("POST").headers(Map.of("Content-Type","application/json")).body(req)
   .willRespondWith().status(201).headers(Map.of("Content-Type","application/json"))
   .body(new PactDslJsonBody().uuid("id",ID).stringValue("laboratorioEquipo","Laboratorio 1").stringValue("descripcion","Equipo sin red")
    .stringValue("prioridad","ALTA").stringValue("fecha","2026-08-31").stringValue("estado","REPORTADO").stringType("creadoEn","2026-08-31T10:00:00Z"))
   .toPact(V4Pact.class);
 }
 @Pact(consumer="scli-mobile") V4Pact listar(PactDslWithProvider b){var body=new PactDslJsonBody();body.minArrayLike("contenido",1).uuid("id",ID).stringValue("estado","REPORTADO").closeObject().closeArray();
  return b.given("usuario autenticado tiene incidentes").uponReceiving("Mobile lista sus incidentes").path("/api/v1/incidentes").method("GET")
   .willRespondWith().status(200).headers(Map.of("Content-Type","application/json")).body(body).toPact(V4Pact.class);}
 @Pact(consumer="scli-mobile") V4Pact registrarToken(PactDslWithProvider b){return b.given("usuario autenticado registra dispositivo")
  .uponReceiving("Mobile registra token FCM").path("/api/v1/notificaciones/dispositivos").method("POST")
  .headers(Map.of("Content-Type","application/json")).body(new PactDslJsonBody().stringValue("token","fcm-token").stringValue("plataforma","ANDROID"))
  .willRespondWith().status(200).headers(Map.of("Content-Type","application/json"))
  .body(new PactDslJsonBody().uuid("id",ID).stringValue("plataforma","ANDROID").booleanValue("activo",true)).toPact(V4Pact.class);}
 @Pact(consumer="scli-mobile") V4Pact desregistrarToken(PactDslWithProvider b){return b.given("usuario autenticado tiene dispositivo registrado")
  .uponReceiving("Mobile da de baja token FCM").path("/api/v1/notificaciones/dispositivos").method("DELETE")
  .headers(Map.of("Content-Type","application/json")).body(new PactDslJsonBody().stringValue("token","fcm-token"))
  .willRespondWith().status(204).toPact(V4Pact.class);}
 private int send(MockServer s,String path,String body)throws Exception{var q=HttpRequest.newBuilder().uri(URI.create(s.getUrl()+path));
  if(body==null)q.GET();else q.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
  return HttpClient.newHttpClient().send(q.build(),HttpResponse.BodyHandlers.ofString()).statusCode();}
 @Test @PactTestFor(pactMethod="crear") void crea(MockServer s)throws Exception{assertEquals(201,send(s,"/api/v1/incidentes","{\"laboratorioEquipo\":\"Laboratorio 1\",\"descripcion\":\"Equipo sin red\",\"prioridad\":\"ALTA\",\"fecha\":\"2026-08-31\"}"));}
 @Test @PactTestFor(pactMethod="listar") void lista(MockServer s)throws Exception{assertEquals(200,send(s,"/api/v1/incidentes",null));}
 @Test @PactTestFor(pactMethod="registrarToken") void registra(MockServer s)throws Exception{assertEquals(200,send(s,"/api/v1/notificaciones/dispositivos","{\"token\":\"fcm-token\",\"plataforma\":\"ANDROID\"}"));}
 @Test @PactTestFor(pactMethod="desregistrarToken") void desregistra(MockServer s)throws Exception{var request=HttpRequest.newBuilder().uri(URI.create(s.getUrl()+"/api/v1/notificaciones/dispositivos"))
  .header("Content-Type","application/json").method("DELETE",HttpRequest.BodyPublishers.ofString("{\"token\":\"fcm-token\"}")).build();
  assertEquals(204,HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString()).statusCode());}
}
