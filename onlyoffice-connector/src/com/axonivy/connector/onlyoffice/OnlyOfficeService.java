package com.axonivy.connector.onlyoffice;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.axonivy.connector.onlyoffice.documenthandler.OnlyOfficeBusinessCaseDocumentHandler;
import com.axonivy.connector.onlyoffice.documenthandler.OnlyOfficeDocumentHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessCallStart;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.util.crypto.CryptoUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@ManagedBean(name = "onlyoffice")
@ApplicationScoped
public class OnlyOfficeService {
	private static final OnlyOfficeService INSTANCE = new OnlyOfficeService();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String VAR_TMPL = "com.axonivy.connector.onlyoffice.%s";
	private static final UUID CLIENT_ID = UUID.fromString("37a13dba-9085-4c3b-a2bf-6694927b75de");
	private static final String ONLYOFFICE_DOCUMENT_PROVIDER_SUBPROCESS_SIGNATURE = "provideOnlyOfficeDocumentHandler()";
	private OnlyOfficeDocumentHandler onlyOfficeDocumentHandler = null;

	public static OnlyOfficeService get() {
		return INSTANCE;
	}

	public OnlyOfficeDocumentHandler getOnlyOfficeDocumentHandler() {
		if(onlyOfficeDocumentHandler == null) {
			Ivy.log().info("Looking for a process that provides the {0} interface.", OnlyOfficeDocumentHandler.class.getCanonicalName());

			Sudo.run(() -> {

				var subProcessStartList = SubProcessCallStart.find(SubProcessSearchFilter.create()
						.setSearchScope(SearchScope.APPLICATION)
						.setSignature(ONLYOFFICE_DOCUMENT_PROVIDER_SUBPROCESS_SIGNATURE)
						.toFilter());

				Object handler = null;

				// Find subprocess
				if (CollectionUtils.isEmpty(subProcessStartList)) {
					handler = OnlyOfficeBusinessCaseDocumentHandler.get();
					Ivy.log().info("No process {0} was provided, using default handler.", ONLYOFFICE_DOCUMENT_PROVIDER_SUBPROCESS_SIGNATURE);
				}
				else {
					var subProcessStart = subProcessStartList.getFirst();

					handler = subProcessStart.call().first();
				}

				if(handler == null) {
					Ivy.log().error("Did not receive a {0}.", OnlyOfficeDocumentHandler.class.getCanonicalName());
				}
				else if(handler instanceof OnlyOfficeDocumentHandler h) {
					onlyOfficeDocumentHandler = h;
					Ivy.log().info("Using {0}: {1}.", OnlyOfficeDocumentHandler.class.getCanonicalName(), handler);
				}
				else {
					Ivy.log().error("Did not receive a {0}, instead received: {1}.", OnlyOfficeDocumentHandler.class.getCanonicalName(), handler);
				}
			});
		}
		return onlyOfficeDocumentHandler;
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
		permissions.put("download", true);
		permissions.put("print", true);
		permissions.put("copy", true);
		permissions.put("fillForms", true);
		permissions.put("modifyFilter", false);
		permissions.put("modifyContentControl", false);

		var document = new LinkedHashMap<String, Object>();
		document.put("fileType", FilenameUtils.getExtension(filename));
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
		// payload.put("documentType", "word");

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
	 * @param editGroup any identifier, all edits in this group can occur in parallel.
	 * @param id
	 *
	 * @return
	 */
	public String createDocumentKey(String editGroup, String documentId) {
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

	public String signConfig(String config) {
		var result = "";
		try {
			var map = MAPPER.readValue(config, new TypeReference<Map<String, Object>>() {});
			map.put("token", createToken(map));
			result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(map);
		} catch (Exception e) {
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

	/**
	 * Parse a given configuration and set the typical dynamic information.
	 *
	 * This includes information about the file and edit group, the user and the signed token.
	 * Whatever is already set, will stay, so it is possible to set your own data, e.g. for the user.
	 *
	 * For a list of configuration items, see https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/
	 * or the comments at the beginning of the ONLYOFFICE editor Javascript.
	 * @param editGroup
	 * @param documentId
	 * @param fileName
	 * @param configuration
	 *
	 * @return
	 */
	public String putIfAbsentAndSign(String editGroup, String documentId, String fileName, String configuration) {
		var result = "";
		try {
			var ivyUser = Ivy.session().getSessionUser();
			var lang = ivyUser.getLanguage();
			if(lang == null) {
				lang = Locale.getDefault();
			}

			var documentKey = createDocumentKey(editGroup, documentId);
			var map = StringUtils.isNotBlank(configuration) ? MAPPER.readValue(configuration, new TypeReference<Map<String, Object>>() {}) : new LinkedHashMap<String, Object>();

			putIfAbsent(map, "document", "fileType", FilenameUtils.getExtension(fileName));
			putIfAbsent(map, "document", "key", documentKey);
			putIfAbsent(map, "document", "title", fileName);
			putIfAbsent(map, "document", "url", getDocumentsBaseUrl("load/{key}", documentKey));
			putIfAbsent(map, "editorConfig", "callbackUrl", getDocumentsBaseUrl("save"));
			putIfAbsent(map, "editorConfig", "lang", lang.getLanguage());
			putIfAbsent(map, "editorConfig", "user", "id", ivyUser.getSecurityMemberId());
			putIfAbsent(map, "editorConfig", "user", "name", ivyUser.getDisplayName());
			putIfAbsent(map, "token", createToken(map));

			result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(map);
		} catch (Exception e) {
			Ivy.log().error("An error occured while creating an ONLYOFFICE configuration for editGroup: ''{0}'' documentId: ''{1}'' fileName: ''{2}'' configuration: ''{3}''",
					e, editGroup, documentId, fileName, configuration);
			result = e.getMessage();
		}
		return result;
	}

	/**
	 * Put the entry into a maps hierarchy and create missing maps.
	 *
	 * @param map
	 * @param params
	 */
	@SuppressWarnings("unchecked")
	public void putIfAbsent(Map<String, Object> map, String...params) {
		if(params.length < 2) {
			throw new IllegalArgumentException("Need at least a key and a value parameter, got %d.".formatted(params.length));
		}

		var p = Arrays.asList(params);

		var value = p.getLast();
		var path = p.subList(0, p.size() - 1);

		var idx = 0;
		var curMap = map;
		while(idx < path.size()) {
			var key = path.get(idx);
			var val = curMap.get(key);
			if(idx < path.size() - 1) {
				// Not the last path component.
				if(val == null || StringUtils.isBlank(val.toString())) {
					var newMap = new LinkedHashMap<String, Object>();
					curMap.put(key, newMap);
					curMap = newMap;
				}
				else {
					curMap = (Map<String, Object>) val;
				}
			}
			else {
				// Last path component.
				if(val == null) {
					curMap.putIfAbsent(key, value);
				}
			}
			idx++;
		}
	}

	public static void main(String[] args) {
		var map = new LinkedHashMap<String, Object>();

		var svc = OnlyOfficeService.get();

		svc.putIfAbsent(map, "docx", "document", "fileType");
		svc.putIfAbsent(map, "test.docs", "document", "title");
		svc.putIfAbsent(map, "User1", "editorConfig", "user", "id");

		System.out.println("Map: %s".formatted(map));
	}
}
