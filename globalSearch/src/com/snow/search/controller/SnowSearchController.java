package com.snow.search.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snow.search.dto.AttributesDTO;
import com.snow.search.dto.ErrorhandlerDTO;
import com.snow.search.dto.MessageDTO;
import com.snow.search.dto.RequestDTO;
import com.snow.search.facade.SnowAutoSuggestFacade;
import com.snow.search.facade.SnowSearchFacade;
import com.snow.search.handler.SnowBasicSearchErrorHandler;
import com.snow.search.handler.SnowErrorHandler;
import com.snow.util.SnowDidYouMeanUtil;
import com.snow.util.SnowPropertiesUtil;

@RestController
public class SnowSearchController {

	public final static Logger LOG = Logger.getLogger(SnowSearchController.class);

	@Resource
	SnowSearchFacade snowSearchFacade;
	
	@Resource
	SnowAutoSuggestFacade snowAutoSuggestFacade;
	
	@RequestMapping(value = "/basicSearch", method = RequestMethod.POST, consumes = "application/json")
	public String basicSolrSearchPost(@RequestBody RequestDTO requestDTO) {
		String queryResponse = null;

		try {
			AttributesDTO attributes = null;
			// Error Handling
			SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
			List<ErrorhandlerDTO> errorhandlerDTOs = basicSearchErrorHandling.errorHandling(requestDTO);
			Iterator<ErrorhandlerDTO> ite = errorhandlerDTOs.iterator();
			while (ite.hasNext()) {
				ErrorhandlerDTO value = ite.next();
				if (value.getErrorCode().equalsIgnoreCase("106"))
					ite.remove();
			}
			if (errorhandlerDTOs.size() != 0 || !errorhandlerDTOs.isEmpty()) {
				MessageDTO messageDTO = populateResponseDTO();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} else {
				String user = snowSearchFacade.fetchUserRoles(requestDTO);
				attributes = snowSearchFacade.fetchAll(requestDTO);
				queryResponse = snowSearchFacade.getQuerySearch(requestDTO, user, attributes);
				// DidyouMean
				SnowDidYouMeanUtil didYouMean = new SnowDidYouMeanUtil();
				if (didYouMean.getNumFound(queryResponse).equalsIgnoreCase("0")) {
					queryResponse = didYouMean.spellcheck(requestDTO.getQueryParam());
				}
				if (null != queryResponse && LOG.isInfoEnabled())
					LOG.info("final Response:" + queryResponse);

			}
		} catch (Exception e) {
			LOG.error("Some error occured ! Please check.", e);
			LOG.error(e.getStackTrace());
			try {
				Properties values = SnowPropertiesUtil.getPropertyValues();
				MessageDTO messageDTO = new MessageDTO();
				messageDTO.setResponseCode(values.getProperty("response.error.responseCode"));
				messageDTO.setResponseMessage(values.getProperty("response.error.responseMessage"));
				List<ErrorhandlerDTO> errorhandlerDTOs = new ArrayList<ErrorhandlerDTO>();
				ErrorhandlerDTO errorhandlerDTO = new ErrorhandlerDTO();
				errorhandlerDTO.setErrorCode(values.getProperty("errorcode.exception"));
				errorhandlerDTO.setErrorMessage(values.getProperty("errorMessage.exception"));
				errorhandlerDTOs.add(errorhandlerDTO);
				SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} catch (JSONException | IOException e1) {
				LOG.error("Some error occured ! Please check.", e);
				queryResponse = "Some Internal Error Occured !";
			}
		}
		return queryResponse;
	}

	private MessageDTO populateResponseDTO() throws IOException {
		MessageDTO messageDTO = new MessageDTO();
		Properties values = SnowPropertiesUtil.getPropertyValues();
		messageDTO.setResponseCode(values.getProperty("response.success.responseCode"));
		messageDTO.setResponseMessage(values.getProperty("response.success.responseMessage"));
		return messageDTO;
	}

	public boolean isUserAuthenticated(String authCredentials) throws IOException {
		Properties values = SnowPropertiesUtil.getPropertyValues();
		String userName = new String(Base64.getDecoder().decode(values.getProperty("serviceNow.user.name")), "UTF-8");
		String password = new String(Base64.getDecoder().decode(values.getProperty("serviceNow.user.password")),
				"UTF-8");
		String userNameAndPassword = userName + ":" + password;
		String encodedCredentials = Base64.getEncoder()
				.encodeToString(userNameAndPassword.getBytes(StandardCharsets.UTF_8));
		String finalCredentials = "Basic " + encodedCredentials;
		if (null != finalCredentials && LOG.isInfoEnabled())
			LOG.info("credentials:" + finalCredentials);

		if (finalCredentials.equals(authCredentials)) {
			return true;
		}
		if (null == authCredentials) {
			return false;
		}
		return false;
	}

	@RequestMapping(value = "/basicSearch", method = RequestMethod.GET, produces = "application/json")
	public String basicSolrSearchGet(@RequestParam String queryParam, @RequestParam Integer start,
			@RequestParam Integer rows, @RequestParam List<String> userRoles, @RequestParam List<String> searchType,
			@RequestParam List<String> facetSelection) {
		String queryResponse = null;

		try {
			RequestDTO requestDTO = new RequestDTO();
			requestDTO.setQueryParam(queryParam);
			requestDTO.setRows(rows);
			requestDTO.setStart(start);
			requestDTO.setSearchType(searchType);
			requestDTO.setUserRoles(userRoles);
			requestDTO.setFacetSelection(facetSelection);
			String user = snowSearchFacade.fetchUserRoles(requestDTO);
			AttributesDTO attributes = null;

			// Error Handling
			SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
			List<ErrorhandlerDTO> errorhandlerDTOs = basicSearchErrorHandling.errorHandling(requestDTO);
			Iterator<ErrorhandlerDTO> ite = errorhandlerDTOs.iterator();
			while (ite.hasNext()) {
				ErrorhandlerDTO value = ite.next();
				if (value.getErrorCode().equalsIgnoreCase("106"))
					ite.remove();
			}
			if (errorhandlerDTOs.size() != 0 || !errorhandlerDTOs.isEmpty()) {
				MessageDTO messageDTO = populateResponseDTO();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} else {
				attributes = snowSearchFacade.fetchAll(requestDTO);
				queryResponse = snowSearchFacade.getQuerySearch(requestDTO, user, attributes);
				if (null != queryResponse && LOG.isInfoEnabled())
					LOG.info("final Response:" + queryResponse);

			}
		} catch (Exception e) {
			LOG.error("Some error occured ! Please check.", e);
			LOG.error(e.getStackTrace());
			try {
				Properties values = SnowPropertiesUtil.getPropertyValues();
				MessageDTO messageDTO = new MessageDTO();
				messageDTO.setResponseCode(values.getProperty("response.error.responseCode"));
				messageDTO.setResponseMessage(values.getProperty("response.error.responseMessage"));
				List<ErrorhandlerDTO> errorhandlerDTOs = new ArrayList<ErrorhandlerDTO>();
				ErrorhandlerDTO errorhandlerDTO = new ErrorhandlerDTO();
				errorhandlerDTO.setErrorCode(values.getProperty("errorcode.exception"));
				errorhandlerDTO.setErrorMessage(values.getProperty("errorMessage.exception"));
				errorhandlerDTOs.add(errorhandlerDTO);
				SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} catch (JSONException | IOException e1) {
				LOG.error("Some error occured ! Please check.", e);
				queryResponse = "Some Internal Error Occured !";
			}
		}
		return queryResponse;
	}

	// AutoSuggestTerms Service
	@RequestMapping(value = "/autoSuggestTerms", method = RequestMethod.POST, consumes = "application/json")
	public String autoSuggestSolrSearchPost(@RequestBody RequestDTO requestDTO) {
		String queryResponse = null;
		try {
			// Error Handling
			SnowErrorHandler errorHandling = new SnowErrorHandler();
			List<ErrorhandlerDTO> errorhandlerDTOs = errorHandling.errorHandling(requestDTO);
			if (errorhandlerDTOs.size() != 0 || !errorhandlerDTOs.isEmpty()) {
				SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
				MessageDTO messageDTO = populateResponseDTO();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} else {
				// AutoFill module
				if (requestDTO.getMode() != null && requestDTO.getMode().equalsIgnoreCase("AutoSuggest")) {
					AttributesDTO attributes = null;
					attributes = snowAutoSuggestFacade.fetchAutoFillFields(requestDTO);
					queryResponse = snowAutoSuggestFacade.getAutoQuerySearch(requestDTO, attributes);
				}
				if (requestDTO.getMode() != null && requestDTO.getMode().equalsIgnoreCase("AutoSuggestTerms")) {
					AttributesDTO attributes = null;
					attributes = snowAutoSuggestFacade.fetchAutoSuggestTerms(requestDTO);
					queryResponse = snowAutoSuggestFacade.autoSuggestTermsQuerySearch(requestDTO, attributes);
				}
			}
		} catch (Exception e) {
			LOG.error("Some error occured ! Please check.", e);
			LOG.error(e.getStackTrace());
			try {
				Properties values = SnowPropertiesUtil.getPropertyValues();
				MessageDTO messageDTO = new MessageDTO();
				messageDTO.setResponseCode(values.getProperty("response.error.responseCode"));
				messageDTO.setResponseMessage(values.getProperty("response.error.responseMessage"));
				List<ErrorhandlerDTO> errorhandlerDTOs = new ArrayList<ErrorhandlerDTO>();
				ErrorhandlerDTO errorhandlerDTO = new ErrorhandlerDTO();
				errorhandlerDTO.setErrorCode(values.getProperty("errorcode.exception"));
				errorhandlerDTO.setErrorMessage(values.getProperty("errorMessage.exception"));
				errorhandlerDTOs.add(errorhandlerDTO);
				SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} catch (JSONException | IOException e1) {
				LOG.error("Some error occured ! Please check.", e);
				queryResponse = "Some Internal Error Occured !";
			}
		}

		return queryResponse;
	}

	@RequestMapping(value = "/autoSuggestTerms", method = RequestMethod.GET, consumes = "application/json")
	public String autoSuggestSolrSearchGet(@RequestParam String queryParam, @RequestParam Integer maxRows,
			@RequestParam List<String> userRoles, @RequestParam List<String> searchType, @RequestParam String mode)
			throws IOException {
		RequestDTO requestDTO = new RequestDTO();
		requestDTO.setQueryParam(queryParam);
		requestDTO.setMaxRows(maxRows);
		requestDTO.setMode(mode);
		requestDTO.setUserRoles(userRoles);
		requestDTO.setSearchType(searchType);
		String queryResponse = null;
		try {
			// Error Handling
			SnowErrorHandler errorHandling = new SnowErrorHandler();
			List<ErrorhandlerDTO> errorhandlerDTOs = errorHandling.errorHandling(requestDTO);
			if (errorhandlerDTOs.size() != 0 || !errorhandlerDTOs.isEmpty()) {
				SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
				MessageDTO messageDTO = populateResponseDTO();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} else {
				// AutoFill module
				if (requestDTO.getMode() != null && requestDTO.getMode().equalsIgnoreCase("AutoSuggest")) {
					AttributesDTO attributes = null;
					attributes = snowAutoSuggestFacade.fetchAutoFillFields(requestDTO);
					queryResponse = snowAutoSuggestFacade.getAutoQuerySearch(requestDTO, attributes);
				}
				if (requestDTO.getMode() != null && requestDTO.getMode().equalsIgnoreCase("AutoSuggestTerms")) {
					AttributesDTO attributes = null;
					attributes = snowAutoSuggestFacade.fetchAutoSuggestTerms(requestDTO);
					queryResponse = snowAutoSuggestFacade.autoSuggestTermsQuerySearch(requestDTO, attributes);
				}
			}
		} catch (Exception e) {
			LOG.error("Some error occured ! Please check.", e);
			LOG.error(e.getStackTrace());
			try {
				Properties values = SnowPropertiesUtil.getPropertyValues();
				MessageDTO messageDTO = new MessageDTO();
				messageDTO.setResponseCode(values.getProperty("response.error.responseCode"));
				messageDTO.setResponseMessage(values.getProperty("response.error.responseMessage"));
				List<ErrorhandlerDTO> errorhandlerDTOs = new ArrayList<ErrorhandlerDTO>();
				ErrorhandlerDTO errorhandlerDTO = new ErrorhandlerDTO();
				errorhandlerDTO.setErrorCode(values.getProperty("errorcode.exception"));
				errorhandlerDTO.setErrorMessage(values.getProperty("errorMessage.exception"));
				errorhandlerDTOs.add(errorhandlerDTO);
				SnowBasicSearchErrorHandler basicSearchErrorHandling = new SnowBasicSearchErrorHandler();
				queryResponse = basicSearchErrorHandling.errorListResponse(errorhandlerDTOs, messageDTO);
			} catch (JSONException | IOException e1) {
				LOG.error("Some error occured ! Please check.", e);
				queryResponse = "Some Internal Error Occured !";
			}
		}
		return queryResponse;
	}
}