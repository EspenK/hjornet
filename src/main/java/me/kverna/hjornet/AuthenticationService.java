package me.kverna.hjornet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import lombok.extern.java.Log;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import javax.annotation.Resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import me.kverna.hjornet.domain.Group;
import me.kverna.hjornet.domain.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.json.JSONObject;

/**
 *
 * @author mikael
 */
@Path("auth")
@Stateless
@Log
public class AuthenticationService {
    private static final String INSERT_USERGROUP = "INSERT INTO AUSERGROUP(NAME,EMAIL) VALUES (?,?)";
    private static final String DELETE_USERGROUP = "DELETE FROM AUSERGROUP WHERE NAME = ? AND EMAIL = ?";

    @Inject
    KeyService keyService;

    @Inject
    IdentityStoreHandler identityStoreHandler;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "issuer")
    String issuer;

    @PersistenceContext
    EntityManager em;

    @Inject
    PasswordHash hasher;

    @Inject
    JsonWebToken principal;

    @Resource(lookup = DatasourceProducer.JNDI_NAME)
    DataSource dataSource;

    /**
     *
     * @param email
     * @param password
     * @param request
     * @return
     */
    @GET
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(
            @QueryParam("email") @NotBlank String email,
            @QueryParam("password") @NotBlank String password,
            @Context HttpServletRequest request) {
        CredentialValidationResult result = identityStoreHandler.validate(
                new UsernamePasswordCredential(email, password));

        JSONObject message = new JSONObject();
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            String token = issueToken(result.getCallerPrincipal().getName(),
                    result.getCallerGroups(), request);
            message.put("token", token);
            return Response.ok(message).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
        } else {
            message.put("message", "user credentials are not valid");
            return Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
        }
    }

    @GET
    @Path("logout")
    @RolesAllowed(value = {Group.USER})
    public Response logout(@Context HttpServletRequest request) throws ServletException
    {
        request.logout();
        return Response.ok().build();
    }

    /**
     *
     * @param name
     * @param groups
     * @param request
     * @return
     */
    private String issueToken(String name, Set<String> groups, HttpServletRequest request) {
        try {
            Date now = new Date();
            Date expiration = Date.from(LocalDateTime.now().plusDays(1L).atZone(ZoneId.systemDefault()).toInstant());
            JwtBuilder jb = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setHeaderParam("kid", "abc-1234567890")
                    .setSubject(name)
                    .setId("a-123")
                    //.setIssuer(issuer)
                    .claim("iss", issuer)
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .claim("upn", name)
                    .claim("groups", groups)
                    .claim("aud", "aud")
                    .claim("auth_time", now)
                    .signWith(keyService.getPrivate());
            return jb.compact();
        } catch (InvalidKeyException t) {
            log.log(Level.SEVERE, "Failed to create token", t);
            throw new RuntimeException("Failed to create token", t);
        }
    }

    /**
     * Create a user.
     *
     * @param user
     * @return
     */
    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(User user) {
        User existingUser = em.find(User.class, user.getEmail());
        if (existingUser == null) {
            user.setPassword(hasher.generate(user.getPassword().toCharArray()));
            Group usergroup = em.find(Group.class, Group.USER);
            user.getGroups().add(usergroup);
            return Response.ok(em.merge(user)).build();
        } else {
            JSONObject message = new JSONObject();
            message.put("message", "user with that email already exists");
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }
    }

    /**
     *
     * @return
     */
    @GET
    @Path("currentuser")    
    @RolesAllowed(value = {Group.USER})
    @Produces(MediaType.APPLICATION_JSON)
    public User getCurrentUser() {
        return em.find(User.class, principal.getName());
    }

    /**
     *
     * @param email
     * @param role
     * @return
     */
    @PUT
    @Path("addrole")
    @RolesAllowed(value = {Group.ADMIN})
    public Response addRole(@QueryParam("email") String email, @QueryParam("role") String role) {
        JSONObject message = new JSONObject();
        if (!roleExists(role)) {
            message.put("message", "role does not exist");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (Connection c = dataSource.getConnection();
             PreparedStatement psg = c.prepareStatement(INSERT_USERGROUP)) {
            psg.setString(1, role);
            psg.setString(2, email);
            psg.executeUpdate();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            message.put("message", ex);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        message.put("message", "role added");
        return Response.ok().entity(message).build();
    }

    /**
     *
     * @param role
     * @return
     */
    private boolean roleExists(String role) {
        boolean result = false;

        if (role != null) {
            switch (role) {
                case Group.ADMIN:
                case Group.USER:
                    result = true;
                    break;
            }
        }

        return result;
    }

    /**
     *
     * @param email
     * @param role
     * @return
     */
    @PUT
    @Path("removerole")
    @RolesAllowed(value = {Group.ADMIN})
    public Response removeRole(@QueryParam("email") String email, @QueryParam("role") String role) {
        if (!roleExists(role)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (Connection c = dataSource.getConnection();
                PreparedStatement psg = c.prepareStatement(DELETE_USERGROUP)) {
            psg.setString(1, role);
            psg.setString(2, email);
            psg.executeUpdate();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    /**
     *
     * @param email
     * @param password
     * @param sc
     * @return
     */
    @PUT
    @Path("changepassword")
    @RolesAllowed(value = {Group.USER})
    public Response changePassword(@QueryParam("email") String email,
            @QueryParam("pwd") String password,
            @Context SecurityContext sc) {
        String authuser = sc.getUserPrincipal() != null ? sc.getUserPrincipal().getName() : null;
        if (authuser == null || email == null || (password == null || password.length() < 3)) {
            log.log(Level.SEVERE, "Failed to change password on user {0}", email);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (authuser.compareToIgnoreCase(email) != 0 && !sc.isUserInRole(Group.ADMIN)) {
            log.log(Level.SEVERE,
                    "No admin access for {0}. Failed to change password on user {1}",
                    new Object[]{authuser, email});
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            User user = em.find(User.class, email);
            user.setPassword(hasher.generate(password.toCharArray()));
            em.merge(user);
            return Response.ok().build();
        }
    }
}
