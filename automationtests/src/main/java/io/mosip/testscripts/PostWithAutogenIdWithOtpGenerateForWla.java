package io.mosip.testscripts;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.admin.fw.util.AdminTestException;
import io.mosip.admin.fw.util.AdminTestUtil;
import io.mosip.admin.fw.util.TestCaseDTO;
import io.mosip.authentication.fw.dto.OutputValidationDto;
import io.mosip.authentication.fw.util.AuthenticationTestException;
import io.mosip.authentication.fw.util.OutputValidationUtil;
import io.mosip.authentication.fw.util.ReportUtil;
import io.mosip.testrunner.HealthChecker;
import io.restassured.response.Response;

public class PostWithAutogenIdWithOtpGenerateForWla extends AdminTestUtil implements ITest {
	private static final Logger logger = Logger.getLogger(PostWithAutogenIdWithOtpGenerateForWla.class);
	protected String testCaseName = "";
	public String idKeyName = null;
	public Response response = null;
	
	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		idKeyName = context.getCurrentXmlTest().getLocalParameters().get("idKeyName");
		logger.info("Started executing yml: "+ymlFile);
		return getYmlTestData(ymlFile);
	}
	

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException {		
		testCaseName = testCaseDTO.getTestCaseName(); 
		if (HealthChecker.signalTerminateExecution == true) {
			throw new SkipException("Target env health check failed " + HealthChecker.healthCheckFailureMapS);
		}
		testCaseName = isTestCaseValidForExecution(testCaseDTO);
		JSONObject req = new JSONObject(testCaseDTO.getInput());
		String otpRequest = null, sendOtpReqTemplate = null, sendOtpEndPoint = null;
		if(req.has("sendOtp")) {
			otpRequest = req.get("sendOtp").toString();
			req.remove("sendOtp");
		}
		JSONObject otpReqJson = new JSONObject(otpRequest);
		sendOtpReqTemplate = otpReqJson.getString("sendOtpReqTemplate");
		otpReqJson.remove("sendOtpReqTemplate");
		sendOtpEndPoint = otpReqJson.getString("sendOtpEndPoint");
		otpReqJson.remove("sendOtpEndPoint");
		
		Response otpResponse = null;
		if(testCaseName.contains("ESignet_")) {
			String tempUrl = ApplnURI.replace("-internal", "");
			otpResponse = postRequestWithCookieAuthHeaderForAutoGenId(tempUrl + sendOtpEndPoint, getJsonFromTemplate(otpReqJson.toString(), sendOtpReqTemplate), COOKIENAME, "mobileauth", testCaseDTO.getTestCaseName(), idKeyName);
		}
		else {
			otpResponse = postWithBodyAndCookie(ApplnURI + sendOtpEndPoint, getJsonFromTemplate(otpReqJson.toString(), sendOtpReqTemplate), COOKIENAME,"resident", testCaseDTO.getTestCaseName());
		}

//		Response otpResponse = postWithBodyAndCookie(ApplnURI + sendOtpEndPoint, getJsonFromTemplate(otpReqJson.toString(), sendOtpReqTemplate), COOKIENAME,"resident", testCaseDTO.getTestCaseName());

		
		JSONObject res = new JSONObject(testCaseDTO.getOutput());
		String sendOtpResp = null, sendOtpResTemplate = null;
		if(res.has("sendOtpResp")) {
			sendOtpResp = res.get("sendOtpResp").toString();
			res.remove("sendOtpResp");
		}
		JSONObject sendOtpRespJson = new JSONObject(sendOtpResp);
		sendOtpResTemplate = sendOtpRespJson.getString("sendOtpResTemplate");
		sendOtpRespJson.remove("sendOtpResTemplate");
		Map<String, List<OutputValidationDto>> ouputValidOtp = OutputValidationUtil
				.doJsonOutputValidation(otpResponse.asString(), getJsonFromTemplate(sendOtpRespJson.toString(), sendOtpResTemplate));
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValidOtp));
		
		if (!OutputValidationUtil.publishOutputResult(ouputValidOtp))
			throw new AdminTestException("Failed at otp output validation");
		
//		JSONObject reqvOtp = new JSONObject(testCaseDTO.getInput());
//		JSONObject reqvtOtp = (JSONObject) reqvOtp.get("sendOtp");
		String otpValidationRequest = null, validateOtpReqTemplate = null, validateOtpEndPoint = null;
		
		if(req.has("validateOtp")) {
			otpValidationRequest = req.get("validateOtp").toString();
			req.remove("validateOtp");
		}
		JSONObject validateOtpReqJson = new JSONObject(otpValidationRequest);
		validateOtpReqTemplate = validateOtpReqJson.getString("validateOtpReqTemplate");
		validateOtpReqJson.remove("validateOtpReqTemplate");
		validateOtpEndPoint = validateOtpReqJson.getString("validateOtpEndPoint");
		validateOtpReqJson.remove("validateOtpEndPoint");
		

//		Response validateOtpResponse = postWithBodyAndCookie(ApplnURI + validateOtpEndPoint, getJsonFromTemplate(validateOtpReqJson.toString(), validateOtpReqTemplate), COOKIENAME,"resident", testCaseDTO.getTestCaseName(), idKeyName);
		Response validateOtpResponse;
		if(testCaseName.contains("ESignet_")) {
			String tempUrl = ApplnURI.replace("-internal", "");
			validateOtpResponse = postRequestWithCookieAuthHeaderForAutoGenId(tempUrl + validateOtpEndPoint, getJsonFromTemplate(validateOtpReqJson.toString(), validateOtpReqTemplate), COOKIENAME, "mobileauth", testCaseDTO.getTestCaseName(), idKeyName);
		}
		
		if(testCaseName.contains("ESignet_")) {
			String tempUrl = ApplnURI.replace("-internal", "");
			response = postWithBodyAndCookieForAutoGeneratedId(tempUrl + testCaseDTO.getEndPoint(), getJsonFromTemplate(req.toString(), testCaseDTO.getInputTemplate()), COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), idKeyName);
		}
		else {
			response = postWithBodyAndCookieForAutoGeneratedId(ApplnURI + testCaseDTO.getEndPoint(), getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate()), COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), idKeyName);
		}
		
//		response = postWithBodyAndCookieForAutoGeneratedId(ApplnURI + testCaseDTO.getEndPoint(), getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate()), COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), idKeyName);
		
		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil
				.doJsonOutputValidation(response.asString(), getJsonFromTemplate(res.toString(), testCaseDTO.getOutputTemplate()));
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));
		
		if (!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");

	}

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}
	
	@AfterClass(alwaysRun = true)
	public void waittime() {
		try {
			if((!testCaseName.contains("ESignet_")) && (!testCaseName.contains("Resident_CheckAidStatus"))) {
				logger.info("waiting for" + props.getProperty("Delaytime")
						+ " mili secs after VID Generation In RESIDENT SERVICES");
				Thread.sleep(Long.parseLong(props.getProperty("Delaytime")));
			}
		} catch (Exception e) {
			logger.error("Exception : " + e.getMessage());
			Thread.currentThread().interrupt();
		}

	}
}
