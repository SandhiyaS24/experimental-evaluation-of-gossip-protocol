package org.ncsu;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.ncsu.entity.Action;
import org.ncsu.entity.ActionRecord;
import org.ncsu.service.ActionService;

import java.util.List;

@Path("/action")
public class ActionResource {

    @Inject
    ActionService actionService;

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response start(List<ActionRecord> actionRecords) {
        boolean initiated = actionService.initiateAction(actionRecords, Action.START);
        if (initiated) {
            return Response.status(Response.Status.OK).build();
        } else  {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/kill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response kill(List<ActionRecord> actionRecords) {
        boolean initiated = actionService.initiateAction(actionRecords, Action.KILL);
        if (initiated) {
            return Response.status(Response.Status.OK).build();
        } else  {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
