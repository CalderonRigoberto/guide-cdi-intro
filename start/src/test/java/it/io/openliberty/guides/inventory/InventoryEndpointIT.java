package it.io.openliberty.guides.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class InventoryEndpointIT {

  private static String port;
  private static String baseUrl;

  private Client client;

  private final String SYSTEM_PROPERTIES = "system/properties";
  private final String INVENTORY_SYSTEMS = "inventory/systems";

  @BeforeAll
  public static void oneTimeSetup() {
    port = System.getProperty("http.port");
    baseUrl = "http://localhost:" + port + "/";
  }

  @BeforeEach
  public void setup() {
    client = ClientBuilder.newClient();
  }

  @AfterEach
  public void teardown() {
    client.close();
  }

  @Test
  @Order(1)
  public void testHostRegistration() {
    this.visitLocalhost();

    Response response = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    this.assertResponse(baseUrl, response);

    JsonObject obj = response.readEntity(JsonObject.class);

    JsonArray systems = obj.getJsonArray("systems");

    boolean localhostExists = false;
    for (int n = 0; n < systems.size(); n++) {
      localhostExists = systems.getJsonObject(n)
                                .get("hostname").toString()
                                .contains("localhost");
      if (localhostExists) {
          break;
      }
    }
    assertTrue(localhostExists,
              "A host was registered, but it was not localhost");

    response.close();
  }

  @Test
  @Order(2)
  public void testSystemPropertiesMatch() {
    Response invResponse = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    Response sysResponse = this.getResponse(baseUrl + SYSTEM_PROPERTIES);

    this.assertResponse(baseUrl, invResponse);
    this.assertResponse(baseUrl, sysResponse);

    JsonObject jsonFromInventory = (JsonObject) invResponse.readEntity(JsonObject.class)
                                                           .getJsonArray("systems")
                                                           .getJsonObject(0)
                                                           .get("properties");

    JsonObject jsonFromSystem = sysResponse.readEntity(JsonObject.class);

    String osNameFromInventory = jsonFromInventory.getString("os.name");
    String osNameFromSystem = jsonFromSystem.getString("os.name");
    this.assertProperty("os.name", "localhost", osNameFromSystem,
                        osNameFromInventory);

    String userNameFromInventory = jsonFromInventory.getString("user.name");
    String userNameFromSystem = jsonFromSystem.getString("user.name");
    this.assertProperty("user.name", "localhost", userNameFromSystem,
                        userNameFromInventory);

    invResponse.close();
    sysResponse.close();
  }

  @Test
  @Order(3)
  public void testUnknownHost() {
    Response response = this.getResponse(baseUrl + INVENTORY_SYSTEMS);
    this.assertResponse(baseUrl, response);

    Response badResponse = client.target(baseUrl + INVENTORY_SYSTEMS + "/"
        + "badhostname").request(MediaType.APPLICATION_JSON).get();

    assertEquals(404, badResponse.getStatus(),
        "BadResponse expected status: 404. Response code not as expected.");

    String obj = badResponse.readEntity(String.class);

    boolean isError = obj.contains("error");
    assertTrue(isError,
              "badhostname is not a valid host but it didn't raise an error");

    response.close();
    badResponse.close();
  }

  private Response getResponse(String url) {
    return client.target(url).request().get();
  }

  private void assertResponse(String url, Response response) {
    assertEquals(200, response.getStatus(), "Incorrect response code from " + url);
  }

  private void assertProperty(String propertyName, String hostname,
      String expected, String actual) {
    assertEquals(expected, actual, "JVM system property [" + propertyName + "] "
        + "in the system service does not match the one stored in "
        + "the inventory service for " + hostname);
  }

  private void visitLocalhost() {
    Response response = this.getResponse(baseUrl + SYSTEM_PROPERTIES);
    this.assertResponse(baseUrl, response);
    response.close();

    Response targetResponse = client.target(baseUrl + INVENTORY_SYSTEMS
        + "/localhost").request().get();
    targetResponse.close();
  }
}