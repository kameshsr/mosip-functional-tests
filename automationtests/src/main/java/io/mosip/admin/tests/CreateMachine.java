package io.mosip.admin.tests;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.admin.fw.util.AdminTestException;
import io.mosip.admin.fw.util.AdminTestUtil;
import io.mosip.authentication.fw.dto.OutputValidationDto;
import io.mosip.authentication.fw.util.AuthenticationTestException;
import io.mosip.authentication.fw.util.DataProviderClass;
import io.mosip.authentication.fw.util.FileUtil;
import io.mosip.authentication.fw.util.OutputValidationUtil;
import io.mosip.authentication.fw.util.ReportUtil;
import io.mosip.authentication.fw.util.RunConfigUtil;
import io.mosip.authentication.fw.util.TestParameters;
import io.mosip.authentication.testdata.TestDataProcessor;
import io.mosip.kernel.service.AssertKernel;
import io.mosip.kernel.util.KernelDataBaseAccess;

public class CreateMachine extends AdminTestUtil implements ITest {

	private static final Logger logger = Logger.getLogger(CreateMachine.class);
	protected String testCaseName = "";
	private String TESTDATA_PATH;
	private String TESTDATA_FILENAME;
	private String testType;
	private int invocationCount = 0;
	AssertKernel ass = new AssertKernel();
	SoftAssert softAssert = new SoftAssert();
	KernelDataBaseAccess masterDB = new KernelDataBaseAccess();

	/**
	 * Set Test Type - Smoke, Regression or Integration
	 * 
	 * @param testType
	 */
	@BeforeClass
	public void setTestType() {
		this.testType = RunConfigUtil.getTestLevel();
		String query = queries.get("createMachine2Eng").toString().replace("true", "false");
		if (masterDB.executeQuery(query, "masterdata"))
			logger.info("created machine with id as Test successfully using query from query.properties");
		else
			logger.info("not able to create machine using query from query.properties");
		
	}

	/**
	 * Method set Test data path and its filename
	 * 
	 * @param index
	 */
	public void setTestDataPathsAndFileNames(int index) {
		this.TESTDATA_PATH = getTestDataPath(getClass().getSimpleName().toString(), index);
		this.TESTDATA_FILENAME = getTestDataFileName(getClass().getSimpleName().toString(), index);
	}

	/**
	 * Method set configuration
	 * 
	 * @param testType
	 */
	public void setConfigurations(String testType) {
		RunConfigUtil.getRunConfigObject("admin");
		RunConfigUtil.objRunConfig.setConfig(this.TESTDATA_PATH, this.TESTDATA_FILENAME, testType);
		TestDataProcessor.initateTestDataProcess(this.TESTDATA_FILENAME, this.TESTDATA_PATH, "admin");
	}

	/**
	 * The method set test case name
	 * 
	 * @param method
	 * @param testData
	 */
	@BeforeMethod
	public void testData(Method method, Object[] testData) {
		String testCase = "";
		if (testData != null && testData.length > 0) {
			TestParameters testParams = null;
			// Check if test method has actually received required parameters
			for (Object testParameter : testData) {
				if (testParameter instanceof TestParameters) {
					testParams = (TestParameters) testParameter;
					break;
				}
			}
			if (testParams != null) {
				testCase = testParams.getTestCaseName();
			}
		}
		testCaseName = String.format(testCase);
		if(!kernelCmnLib.isValidToken(adminCookie))
			adminCookie = kernelAuthLib.getAuthForAdmin();
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[][] getTestCaseList() {
		invocationCount++;
		setTestDataPathsAndFileNames(invocationCount);
		setConfigurations(testType);
		return DataProviderClass.getDataProvider(
				RunConfigUtil.getResourcePath() + RunConfigUtil.objRunConfig.getScenarioPath(),
				RunConfigUtil.objRunConfig.getScenarioPath(), RunConfigUtil.objRunConfig.getTestType());
	}

	/**
	 * Set current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
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
			
			if (testCaseName.contains("All_Valid_Smoke")) {
				if (masterDB.validateDBCount(queries.get("createMachineIsActive").toString(), "masterdata") == 1)
					logger.info("Record inserted in primary language with Status: FALSE");
				else {
					logger.info("Record inserted in primary language with Status: TRUE");
					throw new AdminTestException("Record inserted in one language with Status: TRUE");
				}
			}			
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 * @throws ParseException
	 */
	@Test(dataProvider = "testcaselist")
	public void createMachine(TestParameters objTestParameters, String testScenario, String testcaseName)
			throws AuthenticationTestException, AdminTestException, ParseException {
		File testCaseName = objTestParameters.getTestCaseFile();
		int testCaseNumber = Integer.parseInt(objTestParameters.getTestId());
		displayLog(testCaseName, testCaseNumber);
		setTestFolder(testCaseName);
		setTestCaseId(testCaseNumber);
		setTestCaseName(testCaseName.getName());
		displayContentInFile(testCaseName.listFiles(), "request");
		String url = RunConfigUtil.objRunConfig.getAdminEndPointUrl()
				+ RunConfigUtil.objRunConfig.getCreateMachinePath();
		logger.info("******Post request Json to EndPointUrl: " + url + " *******");
		boolean cookieChanged=false;
		if(this.testCaseName.contains("unAuthorised_role"))
			{
			adminCookie=kernelAuthLib.getAuthForRegistrationOfficer();
			cookieChanged=true;
			}
postRequestAndGenerateOuputFileWithCookie(testCaseName.listFiles(), url, "request", "output-1-actual-response", 0, AUTHORIZATHION_COOKIENAME, adminCookie);
adminCookie = (cookieChanged) ? kernelAuthLib.getAuthForAdmin():adminCookie;
		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doOutputValidation(
				FileUtil.getFilePath(testCaseName, "output-1-actual").toString(),
				FileUtil.getFilePath(testCaseName, "output-1-expected").toString());
		Reporter.log(ReportUtil.getOutputValiReport(ouputValid));
		if(!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");

	}
	/**
	 * this method is for deleting or updating the inserted data in db for testing
	 * (managing class level data not test case level data)
	 * @throws AdminTestException 
	 */
	@AfterClass(alwaysRun = true)
	public void cleanup() throws AdminTestException {
		String historyMsg = "", deleteMsg = "";
		if (masterDB.validateDBCount(queries.get("validateMachineHistory").toString(), "masterdata")==4)
			logger.info("history for Machine validated successfully");
		else {
			
			historyMsg="failed in History validation for create machine";
			logger.info(historyMsg);
		}
			String delMachHistQry = queries.get("deleteMachine2Eng").toString().replace("machine_master", "machine_master_h");
		if (masterDB.executeQuery(queries.get("deleteMachine2Eng").toString(), "masterdata") && 
				masterDB.executeQuery(delMachHistQry, "masterdata"))
			logger.info("deleted all created machine successfully");
		else {
			logger.info("not able to delete machine using query from query.properties");
		}
		if (historyMsg != "" || deleteMsg != "" )
			throw new AdminTestException(historyMsg+" "+deleteMsg);
		}
	}


