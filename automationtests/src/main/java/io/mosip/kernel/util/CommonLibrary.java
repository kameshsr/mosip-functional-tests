package io.mosip.kernel.util;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.Assert;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import io.mosip.admin.fw.util.AdminTestException;
import io.mosip.admin.fw.util.AdminTestUtil;
import io.mosip.global.utils.GlobalConstants;
import io.mosip.kernel.service.ApplicationLibrary;
import io.mosip.service.BaseTestCase;
import io.mosip.testrunner.MosipTestRunner;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

public class CommonLibrary extends BaseTestCase {

	private static Logger logger = Logger.getLogger(CommonLibrary.class);
	private ApplicationLibrary applicationLibrary = new ApplicationLibrary();
	public void checkResponseUTCTime(Response response) {
		logger.info(response.asString());
		JSONObject responseJson = null;
		String responseTime = null;
		try {
			responseJson = (JSONObject) new JSONParser().parse(response.asString());
		} catch (ParseException e1) {
			logger.info(e1.getMessage());
			return;
		}
		if(responseJson!=null && responseJson.containsKey("responsetime"))
			responseTime = response.jsonPath().get("responsetime").toString();
		else return;
		String cuurentUTC = (String) getCurrentUTCTime();
		SimpleDateFormat sdf = new SimpleDateFormat("mm");
		try {
			Date d1 = sdf.parse(responseTime.substring(14, 16));
			Date d2 = sdf.parse(cuurentUTC.substring(14, 16));

			long elapse = Math.abs(d1.getTime() - d2.getTime());
			if (elapse > 300000) {
				Assert.assertTrue(false, "Response time is not UTC, response time : " + responseTime);
			}

		} catch (java.text.ParseException e) {
			logger.error(e.getStackTrace());
		}

	}

	public Object getCurrentUTCTime() {
		String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATEFORMAT);
		LocalDateTime time = LocalDateTime.now(Clock.systemUTC());
		return time.format(dateFormat);
		

	}

	public Object getCurrentLocalTime() {
		String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATEFORMAT);
		LocalDateTime time = LocalDateTime.now();
		return time.format(dateFormat);
		 

	}

	public List<String> getFoldersFilesNameList(String folderRelativePath, boolean isfolder) {
		String configPath = folderRelativePath;
		List<String> listFoldersFiles = new ArrayList<>();

		final File file = new File(getResourcePath() + folderRelativePath);
		logger.info("=====" + getResourcePath() + folderRelativePath);
		logger.info("=======" + file.getAbsolutePath());
		logger.info("=========" + file.getPath());
		for (File f : file.listFiles()) {
			if (f.isDirectory() == isfolder)
				listFoldersFiles.add(configPath + "/" + f.getName());
		}
		return listFoldersFiles;
	}

	public String getResourcePath() {
		return MosipTestRunner.getGlobalResourcePath() + "/";
	}
	
	public String getResourcePathForKernel() {
		return MosipTestRunner.getResourcePath() + "/";
	}

	public JSONObject readJsonData(String path, boolean isRelative) {
		logger.info("path : " + path);
		if(isRelative)
			path = getResourcePath() + path;
		logger.info("Relativepath : " + path);
		FileInputStream inputStream = null;
		JSONObject jsonData = null;
		try {
			File fileToRead = new File(path);
			logger.info("fileToRead : " + fileToRead);
			inputStream = new FileInputStream(fileToRead);
			jsonData = (JSONObject) new JSONParser().parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		} catch (FileNotFoundException e) {
			logger.info("error while reading the file : " + e.getLocalizedMessage() );
			logger.error(e.getStackTrace());
			logger.info("File Not Found at the given path");
		}
		catch (IOException | ParseException | NullPointerException e) {
			logger.info(e.getMessage());
		}finally {
			AdminTestUtil.closeInputStream(inputStream);
		}
		return jsonData;
	}

	public Map<String, String> readProperty(String propertyFileName) {
		Properties prop = new Properties();
		FileInputStream inputStream = null;
		Map<String, String> mapProp = null;
		try {
			logger.info("propertyFileName:  " + propertyFileName + "Path :" + getResourcePathForKernel() + GlobalConstants.CONFIG + propertyFileName + GlobalConstants.DOTPROPERTIES);
			logger.info("propertyFileName:  " + propertyFileName + "Path :" + getResourcePathForKernel() + GlobalConstants.CONFIG + propertyFileName + GlobalConstants.DOTPROPERTIES);
			File propertyFile = new File(getResourcePathForKernel() + GlobalConstants.CONFIG + propertyFileName + GlobalConstants.DOTPROPERTIES);
			inputStream = new FileInputStream(propertyFile);
			prop.load(inputStream);
			mapProp = prop.entrySet().stream()
					.collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
		} catch (IOException e) {
			logger.info("Error occrued while reading propertyFileName " + propertyFileName + e.getMessage());
			logger.info(e.getMessage());
		}finally {
			AdminTestUtil.closeInputStream(inputStream);
		}

		return mapProp;
	}

	public void responseAuthValidation(Response response) {
		JSONArray errors = null;
		String errorCode = null;
		String errorMessage = null;
		int statusCode = response.getStatusCode();
		try {
			if (statusCode > 500)
				Assert.assertTrue(false, "Service is Unavailable and the statusCode=" + statusCode);

			errors = (JSONArray) ((JSONObject) new JSONParser().parse(response.asString())).get("errors");
		} catch (ParseException pe) {
			Assert.assertTrue(false, "Response from the service is not able to read and exception is " + pe.getClass());
		} catch (NullPointerException npe) {
			Assert.assertTrue(false, "Errors in the response is not null and exception is " + npe.getClass());
		}
		if (errors != null) {
			try {
				errorCode = ((JSONObject) errors.get(0)).get("errorCode").toString();
				errorMessage = ((JSONObject) errors.get(0)).get("message").toString();
				if (errorCode.contains("ATH")) {
					Assert.assertTrue(false,
							"Failed due to Authentication failure. Error message is='" + errorMessage + "'");
				}
			} catch (IndexOutOfBoundsException aibe) {
				Assert.assertTrue(false,
						"Not able to find the errorCode or errorMessage from errors array and exception is "
								+ aibe.getClass());
			}
		}
	}
	
	public boolean jsonComparator(String requestJson, String responseJson) throws AdminTestException
	{
		try {
			JSONAssert.assertEquals(requestJson, responseJson, false);
			return true;
		} catch (JSONException |  AssertionError e) {
			logger.info("EXPECTED AND ACTUAL DATA MISMATCH");
			logger.info("MISMATCH DETAILS:");
			logger.info(e.getMessage());
			logger.info("Obtained ACTUAL RESPONSE is:== "+responseJson);
			throw new AdminTestException("Failed at output validation");
		}
	}
	public boolean isValidToken(String cookie) {
		
		logger.info("========= Revalidating the token =========");
		Response response = applicationLibrary.getWithoutParams("/v1/authmanager/authorize/admin/validateToken", cookie);
		JSONObject responseJson =null;
		try {
			responseJson = (JSONObject) ((JSONObject) new JSONParser().parse(response.asString()))
					.get("response");
		} catch (ParseException | NullPointerException e) {
			logger.info(e.getMessage());
		}

		if (responseJson!=null && responseJson.get("errors")==null)
			{
			logger.info("========= Valid Token =========");
			return true;
			}
		else
		{
			
			logger.info("========= InValid Token =========");
			return false;
		}

	}
	
		
		public String removeJsonElement(String readFilePath,List<String> eleToRemove) throws ParseException {
			String jsnString = null;
			String val = null;
			
			try {
				String yourActualJSONString = new String(Files.readAllBytes(Paths.get(readFilePath)), StandardCharsets.UTF_8);
				DocumentContext jsonContext = JsonPath.parse(yourActualJSONString);
				
				for (int i = 0; i < eleToRemove.size(); i++) 
			    {
			    	val=eleToRemove.get(i);
			    	jsonContext.delete(val);
			    	jsnString = jsonContext.jsonString();
			    }
				
			
			} catch (IOException e) {
				logger.error(e.getStackTrace());
			}
			return jsnString;
			
		}

	public void responseLogger(Response response) {
		int statusCode = response.statusCode();
		if (statusCode < 200 || statusCode > 299) {
			logger.info(response.asString());
		} else
			logger.info("status code: " + statusCode + "(success)");

	}

	public Response postWithoutJson(String url, String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().contentType(contentHeader)
				.accept(acceptHeader).log().all().when().post(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithJson(String url, Object body, String contentHeader, String acceptHeader) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Response postResponse = given().relaxedHTTPSValidation().body(body).contentType(contentHeader)
				.accept(acceptHeader).log().all().when().post(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithJson(String url, Object body, String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().body(body)
				.contentType(contentHeader).accept(acceptHeader).log().all().when().post(url).then().log().all()
				.extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithPathParams(String url, Object body, HashMap<String, String> pathParams,
			String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.body(body).contentType(contentHeader).accept(acceptHeader).log().all().when().post(url).then().log()
				.all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}
	
	public Response postWithOnlyPathParams(String url,  HashMap<String, String> pathParams,
			String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.contentType(contentHeader).accept(acceptHeader).log().all().when().post(url).then().log()
				.all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithOnlyFile(String url, File file, String fileKeyName, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().multiPart(fileKeyName, file)
				.expect().when().post(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithFile(String url, Object body, File file, String fileKeyName, String contentHeader,
			String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().multiPart(fileKeyName, file)
				.body(body).contentType(contentHeader).expect().when().post(url).then().log().all().extract()
				.response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithFileFormParams(String url, HashMap<String, String> formParams, File file,
			String fileKeyName, String contentHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		logger.info("Name of the file is" + file.getName());
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().multiPart(fileKeyName, file)
				.formParams(formParams).contentType(contentHeader).expect().when().post(url).then().log().all()
				.extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithFilePathParamsFormParams(String url, HashMap<String, String> pathParams,
			HashMap<String, String> formParams, File file, String fileKeyName, String contentHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		logger.info("Name of the file is" + file.getName());

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.multiPart(fileKeyName, file).formParams(formParams).contentType(contentHeader).expect().when()
				.post(url);
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithQueryParams(String url, HashMap<String, String> queryparams, Object body,
			String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().body(body)
				.queryParams(queryparams).contentType(contentHeader).accept(acceptHeader).log().all().when().post(url)
				.then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postWithMultiHeaders(String endpoint, Object body, HashMap<String, String> headers,
			String contentHeader, String cookie) {
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = given().cookie(builder.build()).headers(headers).relaxedHTTPSValidation()
				.body("\"" + body + "\"").contentType(contentHeader).log().all().when().post(endpoint).then().log()
				.all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + postResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response postRequestEmailNotification(String serviceUri, JSONObject jsonString, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_1 + serviceUri);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response postResponse = null;
		if (jsonString.get(GlobalConstants.ATTACHMENTS).toString().isEmpty()) {
			postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().contentType("multipart/form-data")
					.multiPart(GlobalConstants.MAILCONTENT, (String) jsonString.get(GlobalConstants.MAILCONTENT))
					.multiPart(GlobalConstants.MAILTO, (String) jsonString.get(GlobalConstants.MAILTO))
					.multiPart(GlobalConstants.MAILSUBJECT, (String) jsonString.get(GlobalConstants.MAILSUBJECT))
					.multiPart(GlobalConstants.MAILCC, (String) jsonString.get(GlobalConstants.MAILCC)).post(serviceUri).then().log().all()
					.extract().response();
		} else {
			postResponse = given().cookie(builder.build()).relaxedHTTPSValidation().contentType("multipart/form-data")
					.multiPart(GlobalConstants.ATTACHMENTS, new File(getResourcePath() + (String) jsonString.get(GlobalConstants.ATTACHMENTS)))
					.multiPart(GlobalConstants.MAILCONTENT, (String) jsonString.get(GlobalConstants.MAILCONTENT))
					.multiPart(GlobalConstants.MAILTO, (String) jsonString.get(GlobalConstants.MAILTO))
					.multiPart(GlobalConstants.MAILSUBJECT, (String) jsonString.get(GlobalConstants.MAILSUBJECT))
					.multiPart(GlobalConstants.MAILCC, (String) jsonString.get(GlobalConstants.MAILCC)).post(serviceUri).then().log().all()
					.extract().response();
		}

		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + postResponse.time());
		return postResponse;
	}

	public Response patchRequest(String url, Object body, String contentHeader, String acceptHeader, String cookie) {
		logger.info("REST-ASSURED: Sending a Patch request to " + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response putResponse = given().cookie(builder.build()).relaxedHTTPSValidation().body(body)
				.contentType(contentHeader).accept(acceptHeader).log().all().when().patch(url).then().log().all()
				.extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + putResponse.time());
		return putResponse;
	}

	public Response getWithoutParams(String url, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().log().all().when().get(url);
		responseLogger(getResponse);
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response getWithPathParam(String url, HashMap<String, String> patharams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(patharams).log()
				.all().when().get(url);
		responseLogger(getResponse);
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response getWithQueryParam(String url, HashMap<String, String> queryParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().queryParams(queryParams).log()
				.all().when().get(url);
		responseLogger(getResponse);
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response getWithQueryParamList(String url, HashMap<String, List<String>> queryParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().queryParams(queryParams).log()
				.all().when().get(url);
		responseLogger(getResponse);
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response getWithPathQueryParam(String url, HashMap<String, String> pathParams,
			HashMap<String, String> queryParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.queryParams(queryParams).log().all().when().get(url);
		responseLogger(getResponse);
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response getWithPathParamQueryParamList(String url, HashMap<String, String> pathParams,
			HashMap<String, List<String>> queryParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.queryParams(queryParams).log().all().when().get(url);
		responseLogger(getResponse);
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response putWithoutData(String url, String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_5 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response putResponse = given().cookie(builder.build()).relaxedHTTPSValidation().contentType(contentHeader)
				.accept(acceptHeader).log().all().when().put(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + putResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + putResponse.time());
		return putResponse;
	}

	public Response putWithJson(String url, Object body, String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_5 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response putResponse = given().cookie(builder.build()).relaxedHTTPSValidation().body(body)
				.contentType(contentHeader).accept(acceptHeader).log().all().when().put(url).then().log().all()
				.extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + putResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + putResponse.time());
		return putResponse;
	}

	public Response putWithPathParams(String url, HashMap<String, String> pathParams, String contentHeader,
			String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_5 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response putResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.contentType(contentHeader).accept(acceptHeader).log().all().when().put(url).then().log().all()
				.extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + putResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + putResponse.time());
		return putResponse;
	}

	public Response putWithQueryParams(String url, HashMap<String, String> queryParams, String contentHeader,
			String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_5 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response putResponse = given().cookie(builder.build()).relaxedHTTPSValidation().queryParams(queryParams)
				.contentType(contentHeader).accept(acceptHeader).log().all().when().put(url).then().log().all()
				.extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + putResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + putResponse.time());
		return putResponse;
	}

	public Response putWithPathParamsBody(String url, HashMap<String, String> pathParams, Object body,
			String contentHeader, String acceptHeader, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_5 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response putResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.body(body).contentType(contentHeader).accept(acceptHeader).log().all().when().put(url).then().log()
				.all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + putResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + putResponse.time());
		return putResponse;
	}

	public Response deleteWithPathParams(String url, HashMap<String, String> pathParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_6 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams).log()
				.all().when().delete(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + getResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	
	public Response deleteWithQueryParams(String url, HashMap<String, String> queryParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_6 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().queryParams(queryParams).log()
				.all().when().delete(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + getResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	
	public Response deleteWithPathQueryParams(String url, HashMap<String, String> pathParams,
			HashMap<String, String> queryParams, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_6 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().pathParams(pathParams)
				.queryParams(queryParams).log().all().when().delete(url).then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + getResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	
	public Response getConfigProperties(String url) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_4 + url);
		Response getResponse = given().relaxedHTTPSValidation().log().all().when().get(url).then().log().all().extract()
				.response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response deleteWithoutParams(String url, String cookie) {
		logger.info(GlobalConstants.REST_ASSURED_STRING_6 + url);
		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		Response getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().log().all().when().delete(url)
				.then().log().all().extract().response();
		logger.info(GlobalConstants.REST_ASSURED_STRING_2 + getResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}

	public Response postJSONwithFile(Object body, File file, String url, String contentHeader, String cookie) {
		Response getResponse = null;

		String Document_request = readProperty("IDRepo").get("req.Documentrequest");

		Cookie.Builder builder = new Cookie.Builder(GlobalConstants.AUTHORIZATION, cookie);
		getResponse = given().cookie(builder.build()).relaxedHTTPSValidation().multiPart("file", file)
				.formParam(Document_request, body).contentType(contentHeader).expect().when().post(url);
		logger.info("REST:ASSURED: The response from request is:" + getResponse.asString());
		logger.info(GlobalConstants.REST_ASSURED_STRING_3 + getResponse.time());
		return getResponse;
	}
}
