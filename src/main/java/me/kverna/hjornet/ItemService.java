package me.kverna.hjornet;

import lombok.extern.java.Log;
import me.kverna.hjornet.domain.Group;
import me.kverna.hjornet.domain.Item;
import me.kverna.hjornet.domain.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.json.simple.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("item")
@Stateless
@Log
public class ItemService {
    private static final String ALL_ITEMS = "SELECT i FROM Item i";

    @PersistenceContext
    EntityManager em;

    @Inject
    JsonWebToken principal;

    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed(value = {Group.USER})
    public Response create(Item item) {
        item.setOwner(em.find(User.class, principal.getName()));
        return Response.ok(em.merge(item)).build();
    }

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        Query query = em.createQuery(ALL_ITEMS);
        return Response.ok(query.getResultList()).build();
    }

    @PATCH
    @Path("buy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buy(@QueryParam("id") @NotNull int id) {
        Item item = em.find(Item.class, id);
        if (item == null) {
            JSONObject message = new JSONObject();
            message.put("message", "item does not exist");
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        if (item.getBuyer() != null) {
            JSONObject message = new JSONObject();
            message.put("message", "somebody already bought this item");
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        User buyer = em.find(User.class, principal.getName());
        if (buyer.equals(item.getOwner())) {
            JSONObject message = new JSONObject();
            message.put("message", "you can not buy your own item");
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        item.setBuyer(buyer);
        JSONObject message = new JSONObject();
        message.put("message", "item bought successfully");
        return Response.ok(item).entity(message).build();
    }
}
