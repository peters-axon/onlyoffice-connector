package com.axonivy.connector.onlyoffice;

import java.io.InputStream;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.Ivy;

@Path("/documents")
public class OnlyOfficeResource {
	/**
	 * Paths which will not require the X-Requested-By header.
	 */
	private static final Set<String> CSRF_EXCEPTIONS = Set.of(
			"/documents/save"
			);

	/**
	 * Paths which will not require the X-Requested-By header.
	 *
	 * @param pathInfo
	 * @return
	 */
	public static boolean isCsrfException(String pathInfo) {
		return CSRF_EXCEPTIONS.contains(pathInfo);
	}

	@GET
	@Path("load/{key}")
	@PermitAll
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response loadDocument(@Context HttpServletRequest rq, @PathParam("key") String key) {
		var dei = OnlyOfficeService.get().extractDocumentEditId(key);
		var documentId = dei.documentId();
		Ivy.log().debug("Load call for document: {0}", documentId);

		var doc = OnlyOfficeService.get().getOnlyOfficeDocumentHandler().load(dei.editGroup(), dei.documentId());

		if (doc == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		return Response.ok(doc.getStream())
				.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(doc.getFileName()))
				.build();
	}

	@POST
	@Path("save")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveDocument(@Context HttpServletRequest rq, JsonNode payload) {
		Ivy.log().debug("Save call for document: {0}", payload);

		try {
			var status = payload.get("status").asInt();
			switch(status) {
			case 2:
			case 6:
				var key = payload.get("key").asText();
				var url = payload.get("url").asText();

				var intUrl = OnlyOfficeService.get().toInternalUrl(url);
				Ivy.log().debug("Converting URL to internal: original: {0} internal: {1}", url, intUrl);

				var client = OnlyOfficeService.get().absolute(intUrl);

				var rsp = client.request().get();

				var stream = rsp.readEntity(InputStream.class);

				var dei = OnlyOfficeService.get().extractDocumentEditId(key);

				Ivy.log().debug("Save Document: {0}", dei.documentId());

				var doc = OnlyOfficeDocument.builder().editGroup(dei.editGroup()).documentId(dei.documentId()).stream(stream).build();

				OnlyOfficeService.get().getOnlyOfficeDocumentHandler().save(doc, status == 2);

				Ivy.log().debug("Document was saved: {0}", dei.documentId());

				break;
			default:
				break;
			}
		} catch (Exception e) {
			Ivy.log().error("Exception during Document save.", e);
		}
		return Response.ok(OnlyOfficeResult.OK).build();
	}
}
