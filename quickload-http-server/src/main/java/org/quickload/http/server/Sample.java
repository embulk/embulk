package org.quickload.http.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v0.1.0/quick_load")
public class Sample
{
    @GET
    @Path("source_list")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPartyCount() throws Exception
    {
        return String.valueOf(1);
    }
}
