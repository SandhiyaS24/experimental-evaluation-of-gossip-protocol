package org.ncsu.service;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Path;

@Path("/action")
@RegisterRestClient
public interface NodeManagerService {

    @POST
    @Path("/start")
    Response startNode(@HeaderParam("action-port") Integer actionPort,
                       @HeaderParam("peer") String peer,
                       @HeaderParam("kafka-topic") String kafkaTopic,
                       @HeaderParam("kafka-broker") String kafkaBroker);

    @POST
    @Path("/start")
    Response killNode(@HeaderParam("action-port") Integer actionPort);
}