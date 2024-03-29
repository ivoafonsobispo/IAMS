package ipleiria.dae.project.ws;

import ipleiria.dae.project.dtos.OccurrenceDTO;
import ipleiria.dae.project.dtos.PaginatedDTOs;
import ipleiria.dae.project.ejbs.OccurrenceBean;
import ipleiria.dae.project.entities.Occurrence;
import ipleiria.dae.project.exceptions.APIBadResponseException;
import ipleiria.dae.project.exceptions.MyEntityNotFoundException;
import ipleiria.dae.project.exceptions.NotAuthorizedException;
import ipleiria.dae.project.requests.PageRequest;
import ipleiria.dae.project.security.Authenticated;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.List;

@Path("occurrences")
@Produces({MediaType.APPLICATION_JSON}) // injects header “Content-Type: application/json”
@Consumes({MediaType.APPLICATION_JSON})
public class OccurrenceService {
    @EJB
    private OccurrenceBean occurrenceBean;
    @Context
    private SecurityContext securityContext;

    @GET
    @Path("/")
    public Response getAllOccurrences(@BeanParam @Valid PageRequest pageRequest) {
        var count = occurrenceBean.count();
        var offset = pageRequest.getOffset();
        var limit = pageRequest.getLimit();

        if (offset > count) {
            return Response.ok(new PaginatedDTOs<>(count)).build();
        }

        var occurrences = occurrenceBean.getAll(limit, pageRequest.getPage());
        var paginatedDTO = new PaginatedDTOs<>(OccurrenceDTO.from(occurrences), count, offset, limit);

        return Response.ok(paginatedDTO).build();
    }

    @GET
    @Path("/insuranceCompany/{insuranceCompany}")
    public Response getAllOccurrences(@BeanParam @Valid PageRequest pageRequest, @PathParam("insuranceCompany") String insuranceCompany) {
        var count = occurrenceBean.countByInsuranceCompany(insuranceCompany);
        var offset = pageRequest.getOffset();
        var limit = pageRequest.getLimit();

        if (offset > count) {
            return Response.ok(new PaginatedDTOs<>(count)).build();
        }

        var occurrences = occurrenceBean.getAllByInsuranceCompany(limit, pageRequest.getPage(), insuranceCompany);
        var paginatedDTO = new PaginatedDTOs<>(OccurrenceDTO.from(occurrences), count, offset, limit);

        return Response.ok(paginatedDTO).build();
    }

    @GET
    @Path("/{id}")
    public Response getOccurrenceDetails(@PathParam("id") long id) {
        Occurrence occurrence = occurrenceBean.find(id);
        if (occurrence != null) {
            return Response.ok().entity(OccurrenceDTO.from(occurrence)).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("ERROR_FINDING_OCCURRENCE")
                .build();
    }

    @POST
    @Authenticated
    @RolesAllowed({"Client"})
    @Path("/")
    public Response create(OccurrenceDTO occurrenceDTO) throws MyEntityNotFoundException, APIBadResponseException, NotAuthorizedException {
        if (!securityContext.getUserPrincipal().getName().equals(occurrenceDTO.getUsernameClient())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Occurrence occurrence = occurrenceBean.create(
                occurrenceDTO.getUsernameClient(),
                occurrenceDTO.getEntryDate(),
                occurrenceDTO.getState(),
                occurrenceDTO.getInsuranceCode(),
                occurrenceDTO.getCoverageType(),
                occurrenceDTO.getDescription()
        );

        if (occurrence == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.CREATED)
                .entity(OccurrenceDTO.from(occurrence))
                .build();
    }

    @PUT
    @Authenticated
    @RolesAllowed({"Client", "Administrator"})
    @Path("/{id}")
    public Response updateOccurrence(@PathParam("id") long id, OccurrenceDTO occurrenceDTO) throws MyEntityNotFoundException, APIBadResponseException, NotAuthorizedException {
        if (!securityContext.getUserPrincipal().getName().equals(occurrenceDTO.getUsernameClient())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Occurrence occurrence;
        occurrence = occurrenceBean.update(
                id,
                occurrenceDTO.getUsernameClient(),
                occurrenceDTO.getEntryDate(),
                occurrenceDTO.getState(),
                occurrenceDTO.getInsuranceCode(),
                occurrenceDTO.getCoverageType(),
                occurrenceDTO.getDescription()
        );

        if (occurrence == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.OK)
                .entity(OccurrenceDTO.from(occurrence))
                .build();
    }

//    @GET
//    @Path("/{id}/document")
//    public Response getOccurrenceDocuments(@PathParam("id") long id){
//        return Response.status(Response.Status.BAD_REQUEST).build();
//    }
//
//    @PUT
//    @Path("/{id}/document")
//    public Response addDocumentsToOccurrence(@PathParam("id") long id){
//        return Response.status(Response.Status.BAD_REQUEST).build();
//    }

    @DELETE
    @Authenticated
    @RolesAllowed({"Client", "Administrator"})
    @Path("/{id}")
    public Response deleteOccurrence(@PathParam("id") long id) throws MyEntityNotFoundException {
        occurrenceBean.delete(id);

        return Response.noContent().build();
    }

    /*private List<OccurrenceDTO> toDTOS(List<Occurrence> allOccurrences) {
        return allOccurrences.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private OccurrenceDTO toDTO(Occurrence occurrence){

        return new OccurrenceDTO(
                occurrence.getId(),
                occurrence.getClient().getUsername(),
                occurrence.getDate(),
                occurrence.getState(),
                occurrence.getInsuredAssetType(),
                occurrence.getInsurance().getCode(),
                occurrence.getInsurance().getName(),
                occurrence.getDescription(),
                occurrence.getObject()
        );
    }*/

}
