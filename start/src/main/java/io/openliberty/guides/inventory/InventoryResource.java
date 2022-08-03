package io.openliberty.guides.inventory;

import java.util.Properties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.client.SystemClient;

@ApplicationScoped
@Path("/systems")
public class InventoryResource {

  @Inject
  InventoryManager manager;

  @Inject
  SystemClient systemClient;

  @GET
  @Path("/{hostname}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPropertiesForHost(@PathParam("hostname") String hostname) {
    Properties props = systemClient.getProperties(hostname);
    if (props == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("{ \"error\" : \"Unknown hostname " + hostname
                             + " or the inventory service may not be running "
                             + "on the host machine \" }")
                     .build();
    }

    manager.add(hostname, props);
    return Response.ok(props).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public InventoryList listContents() {
    return manager.list();
  }
}