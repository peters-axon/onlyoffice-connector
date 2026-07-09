package com.axonivy.connector.onlyoffice;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.util.crypto.CryptoUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class OnlyOfficeService {
	private static final OnlyOfficeService INSTANCE = new OnlyOfficeService();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String VAR_TMPL = "com.axonivy.connector.onlyoffice.%";
	private static final UUID CLIENT_ID = UUID.fromString("8ebb698e-6dc2-4bca-938c-231642f4aa6e");

	public static OnlyOfficeService get() {
		return INSTANCE;
	}

	public WebTarget absolute(String url) {
		return Ivy.rest().client(CLIENT_ID).property(OnlyOfficeFeature.CONFIG_KEY_URL, url);
	}

	public String documentsBaseUrl() {
		return getVar("documentsBaseUrl");
	}

	public String documentServerInternalBaseUrl() {
		return getVar("documentServerInternalBaseUrl");
	}

	public String documentServerExternalBaseUrl() {
		return getVar("documentServerExternalBaseUrl");
	}

	public String onlyOfficeJwtsecret() {
		return getVar("jwtsecret");
	}

	public String getVar(String name) {
		return Ivy.var().get(VAR_TMPL.formatted(name));
	}

	public String getDocumentsBaseUrl(String path, Object...values) {
		var base = documentsBaseUrl();
		return UriBuilder.fromUri(base).path(path).build(values).toString();
	}

	public WebTarget documentServerInternal(String path) {
		var base = documentServerInternalBaseUrl();
		var url = UriBuilder.fromUri(base).path(path).build().toString();
		return absolute(url);
	}

	public String getDocumentEditorExternalUrl() {
		var base = documentServerExternalBaseUrl();
		return UriBuilder.fromUri(base).path("web-apps/apps/api/documents/api.js").build().toString();
	}

	public String toInternalUrl(String url) {
		var intBase= UriBuilder
				.fromUri(documentServerInternalBaseUrl())
				.build((Object[])null);
		return UriBuilder
				.fromUri(url)
				.scheme(intBase.getScheme())
				.host(intBase.getHost())
				.port(intBase.getPort())
				.toString();
	}

	public WebTarget command() {
		return documentServerInternal("command");
	}

	public String createToken(Map<String, Object> config) {
		var secret = onlyOfficeJwtsecret();
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
				.setClaims(config)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1h
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	/**
	 * Create standard configuration for editing.
	 *
	 * @param documentKey
	 * @param filename
	 * @return
	 */
	public Map<String,Object> createStandardConfig(String documentKey, String filename) {
		var permissions = new LinkedHashMap<String, Object>();
		permissions.put("edit", true);
		permissions.put("review", true);
		permissions.put("download", false);
		permissions.put("print", false);
		permissions.put("copy", false);
		permissions.put("fillForms", true);
		permissions.put("modifyFilter", false);
		permissions.put("modifyContentControl", false);

		var document = new LinkedHashMap<String, Object>();
		document.put("fileType", "docx");
		document.put("key", documentKey);
		document.put("title", filename);
		document.put("url", getDocumentsBaseUrl("load/{key}", documentKey));
		document.put("permissions", permissions);

		var customization = new LinkedHashMap<String, Object>();
		customization.put("forcesave", true);
		customization.put("autosave", true);
		customization.put("trackChanges", true);
		customization.put("chat", false);
		customization.put("comments", false);
		customization.put("compactToolbar", false);
		customization.put("hideRightMenu", true);
		//		customization.put("integrationMode", "embed");
		//customization.put("uiTheme", "theme-light");

		var ivyUser = Ivy.session().getSessionUser();
		var user = new LinkedHashMap<String, Object>();
		user.put("id", ivyUser.getSecurityMemberId());
		user.put("name", ivyUser.getDisplayName());

		var editorConfig = new LinkedHashMap<String, Object>();
		editorConfig.put("callbackUrl", getDocumentsBaseUrl("save"));
		editorConfig.put("lang", "en");
		editorConfig.put("mode", "edit");
		editorConfig.put("customization", customization);
		editorConfig.put("user", user);

		var payload = new LinkedHashMap<String, Object>();
		payload.put("document", document);
		payload.put("editorConfig", editorConfig);
		payload.put("documentType", "word");

		return payload;
	}

	public static record DocumentEditId(String documentId, String editGroup) {
		public static DocumentEditId create(String documentId, String editGroup) {
			return new DocumentEditId(documentId, editGroup);
		}
		public static DocumentEditId fromList(List<String> list) {
			return create(list.get(0), list.get(1));
		}
		public List<String> toList() {
			return List.of(documentId, editGroup);
		}
	}

	/**
	 * Create a document key for the document.
	 *
	 * @param id
	 * @param editGroup any identifier, all edits in this group can occur in parallel.
	 * @return
	 */
	public String createDocumentKey(String documentId, String editGroup) {
		try {
			var key = MAPPER.writeValueAsString(DocumentEditId.create(documentId, editGroup).toList());
			var enc = CryptoUtil.encrypt(key);
			var bin = Base64.getDecoder().decode(enc);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(bin);
		} catch (Exception e) {
			throw new RuntimeException("Could not create key for documentId: '%s' editGroup: '%s'".formatted(documentId, editGroup), e);
		}
	}

	/**
	 * Extract the information contained in a document key.
	 *
	 * @param documentKey
	 * @return
	 */
	public DocumentEditId extractDocumentEditId(String documentKey) {
		try {
			var bin = Base64.getUrlDecoder().decode(documentKey);
			var enc = Base64.getEncoder().encodeToString(bin);
			var packed = CryptoUtil.decrypt(enc);
			return DocumentEditId.fromList(MAPPER.readValue(packed, new TypeReference<List<String>>() {}));
		} catch (Exception e) {
			throw new RuntimeException("Could not extract from '%s'".formatted(documentKey), e);
		}
	}

	/**
	 * Create the config script.
	 *
	 * @param documentKey
	 * @param filename
	 * @return
	 */
	public String createConfigScript(String documentKey, String filename) {
		var config = createStandardConfig(documentKey, filename);
		config.put("token", createToken(config));
		var result = "";
		try {
			result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(config);
		} catch (JsonProcessingException e) {
			result = ExceptionUtils.getStackTrace(e);
		}
		return result;
	}

	public Response callForcesave(String key, String userdata) {
		var cmd = new LinkedHashMap<String, Object>();
		cmd.put("c", "forcesave");
		cmd.put("key", key);
		cmd.put("userdata", userdata);

		var token = createToken(cmd);

		var node = MAPPER.createObjectNode();
		node.put("token", token);
		return command().request().buildPost(Entity.entity(node, MediaType.APPLICATION_JSON)).invoke();
	}

}
