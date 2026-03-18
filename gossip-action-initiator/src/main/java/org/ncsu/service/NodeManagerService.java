package org.ncsu.service;

import jakarta.json.JsonObject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

@Path("/action")
@RegisterRestClient
public interface NodeManagerService {

    @POST
    @Path("/start")
    Response startNode(@HeaderParam("action-port") Integer actionPort);

    @POST
    @Path("/start")
    Response killNode(@HeaderParam("action-port") Integer actionPort);
}