package io.mosip.testrig.apirig.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import io.mosip.testrig.apirig.admin.fw.util.AdminTestUtil;
import io.mosip.testrig.apirig.service.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.MosipTestRunner;

/**
 * Customised Testng Report
 * 
 * @author Vignesh
 *
 */
public class CustomTestNGReporter extends Reporter implements IReporter {

	private static final Logger CustomTestNGReporterLog = Logger.getLogger(CustomTestNGReporter.class);
	private static final String emailableReportTemplateFile = new File(
			MosipTestRunner.getGlobalResourcePath()+"/customize-emailable-report-template.html").getAbsolutePath();
	private static StringBuffer customReportTemplateStr = new StringBuffer();

	private static final String reportProfixFileName = "MOSIP_ModuleLevelAutoRun_TestNGReport";

	// PieChart
	private int passTestCount = 0;
	private int skipTestCount = 0;
	private int failTestCount = 0;
	private int totalCount = 0;
	private String color = "";
	private int countTestClassName = 0;
	private boolean testClassNameFlag = false;

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		FileWriter fileWriter = null;
		try {
			customReportTemplateStr = this.readEmailabelReportTemplate();
			customReportTemplateStr.append(this.getCustomReportTitle("MOSIP API Test Report"));
			customReportTemplateStr.append(afterTestReportTittleContent());
			customReportTemplateStr.append(this.getTestSuiteSummary(suites));
			customReportTemplateStr.append(afterTestCaseSummaryContent());
			customReportTemplateStr.append(this.getTestMethodSummary(suites));
			customReportTemplateStr.append(afterTestMethodSummaryContent());
			String finalcustomReport = updatePieChart(customReportTemplateStr.toString());

			removeOldCustomMosipReport(outputDirectory);

			File targetFile = new File(outputDirectory + "/"+reportProfixFileName/*getCurrentDateForReport()*/+".html");
			fileWriter = new FileWriter(targetFile);
			fileWriter.write(finalcustomReport);
		} catch (NullPointerException | IOException e) {
			CustomTestNGReporterLog.error(e.getMessage());
		} finally {
			AdminTestUtil.closeFileWriter(fileWriter);
		}
	}

	private String updatePieChart(String customReportTemplateStr) {
		customReportTemplateStr = customReportTemplateStr.replaceAll("\\$pass\\$", String.valueOf(passTestCount));
		customReportTemplateStr = customReportTemplateStr.replaceAll("\\$skip\\$", String.valueOf(skipTestCount));
		customReportTemplateStr = customReportTemplateStr.replaceAll("\\$fail\\$", String.valueOf(failTestCount));
		return customReportTemplateStr;
	}

	private StringBuffer readEmailabelReportTemplate() {
		StringBuffer retBuf = new StringBuffer();
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {

			File file = new File(emailableReportTemplateFile);
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);

			String line = bufferedReader.readLine();
			while (line != null) {
				retBuf.append(line);
				line = bufferedReader.readLine();
			}

		} catch (NullPointerException | IOException e) {
			CustomTestNGReporterLog.error(e.getMessage());
		} finally {
			AdminTestUtil.closeBufferedReader(bufferedReader);
			AdminTestUtil.closeFileReader(fileReader);
		}
		return retBuf;
	}

	private String getCustomReportTitle(String title) {
		StringBuffer retBuf = new StringBuffer();
		retBuf.append(title + " " + this.getDateInStringFormat(new Date())+" ENV: "+BaseTestCase.ApplnURI);
		return retBuf.toString();
	}

	private String getTestSuiteSummary(List<ISuite> suites) {
		StringBuffer retBuf = new StringBuffer();

		try {
			int totalTestCount = 0;
			int totalTestPassed = 0;
			int totalTestFailed = 0;
			int totalTestSkipped = 0;

			for (ISuite tempSuite : suites) {
				retBuf.append("<tr><td colspan=11><center><b>" + tempSuite.getName() + "</b></center></td></tr>");

				Map<String, ISuiteResult> testResults = tempSuite.getResults();

				for (ISuiteResult result : testResults.values()) {

					retBuf.append("<tr>");

					ITestContext testObj = result.getTestContext();

					totalTestPassed = testObj.getPassedTests().getAllMethods().size();
					totalTestSkipped = testObj.getSkippedTests().getAllMethods().size();
					totalTestFailed = testObj.getFailedTests().getAllMethods().size();

					totalTestCount = totalTestPassed + totalTestSkipped + totalTestFailed;

					retBuf.append("<td>");
					retBuf.append(testObj.getName());
					retBuf.append("</td>");

					retBuf.append("<td>");
					retBuf.append(totalTestCount);
					totalCount = totalCount + totalTestCount;
					retBuf.append("</td>");

					retBuf.append("<td bgcolor=#3cb353>");
					retBuf.append(totalTestPassed);
					passTestCount = passTestCount + totalTestPassed;
					retBuf.append("</td>");

					retBuf.append("<td bgcolor=#EEE8AA>");
					retBuf.append(totalTestSkipped);
					skipTestCount = skipTestCount + totalTestSkipped;
					retBuf.append("</td>");

					retBuf.append("<td bgcolor=#FF4500>");
					retBuf.append(totalTestFailed);
					failTestCount = failTestCount + totalTestFailed;
					retBuf.append("</td>");

					Date startDate = testObj.getStartDate();
					retBuf.append("<td>");
					retBuf.append(this.getTimeInStringFormat1(startDate));
					retBuf.append("</td>");

					Date endDate = testObj.getEndDate();
					retBuf.append("<td>");
					retBuf.append(this.getTimeInStringFormat1(endDate));
					retBuf.append("</td>");

					long deltaTime = endDate.getTime() - startDate.getTime();
					String deltaTimeStr = this.convertDeltaTimeToStringInHhMmSs(deltaTime);
					retBuf.append("<td>");
					retBuf.append(deltaTimeStr);
					retBuf.append("</td>");

					String deploymentVersion = getAppDepolymentVersion();
					retBuf.append("<td>");
					retBuf.append(deploymentVersion);
					retBuf.append("</td>");


					retBuf.append("</tr>");
				}
			}
			retBuf.append("<tr>");

			retBuf.append("<td>");
			retBuf.append("Total Execution Count");
			retBuf.append("</td>");

			retBuf.append("<td>");
			retBuf.append(totalCount);
			retBuf.append("</td>");

			retBuf.append("<td bgcolor=#3cb353>");
			retBuf.append(passTestCount);
			retBuf.append("</td>");

			retBuf.append("<td bgcolor=#EEE8AA>");
			retBuf.append(skipTestCount);
			retBuf.append("</td>");

			retBuf.append("<td bgcolor=#FF4500>");
			retBuf.append(failTestCount);
			retBuf.append("</td>");

			retBuf.append("<tr>");
		} catch (Exception e) {
			CustomTestNGReporterLog.error(e.getMessage());
		}
		return retBuf.toString();
	}

	private String getDateInStringFormat(Date date) {
		StringBuffer retBuf = new StringBuffer();
		if (date == null) {
			date = new Date();
		}
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		retBuf.append(df.format(date));
		return retBuf.toString();
	}
	
	private String getTimeInStringFormat1(Date date) {
		StringBuffer retBuf = new StringBuffer();
		if (date == null) {
			date = new Date();
		}
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		retBuf.append(df.format(date));
		return retBuf.toString();
	}

	private String getTimeInStringFormat(Date date) {
		StringBuffer retBuf = new StringBuffer();
		if (date == null) {
			date = new Date();
		}
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		retBuf.append(df.format(date));
		return retBuf.toString();
	}

	private String convertDeltaTimeToString(long deltaTime) {
		StringBuffer retBuf = new StringBuffer();
		long milli = deltaTime;

		String milliSec = "0";
		if (String.valueOf(milli).length() > 3)
			milliSec = String.valueOf(milli).substring(0, 3);
		else
			milliSec = String.valueOf(milli);
		long seconds = deltaTime / 1000 % 60;
		long minutes = deltaTime / (60 * 1000) % 60;
		long hours = deltaTime / (60 * 60 * 1000) % 24;
		retBuf.append(hours + ":" + minutes + ":" + seconds + ":" + milliSec);
		return retBuf.toString();
	}

	private String convertDeltaTimeToStringInHhMmSs(long deltaTime) {
		StringBuffer retBuf = new StringBuffer();
		long milli = deltaTime;
		long seconds = deltaTime / 1000 % 60;
		long minutes = deltaTime / (60 * 1000) % 60;
		long hours = deltaTime / (60 * 60 * 1000) % 24;

		retBuf.append(hours + ":" + minutes + ":" + seconds);
		return retBuf.toString();
	}

	private String getTestMethodSummary(List<ISuite> suites) {
		StringBuffer retBuf = new StringBuffer();

		try {
			for (ISuite tempSuite : suites) {
				retBuf.append("<tr><td colspan=7><center><b>" + tempSuite.getName() + "</b></center></td></tr>");

				Map<String, ISuiteResult> testResults = tempSuite.getResults();

				for (ISuiteResult result : testResults.values()) {

					ITestContext testObj = result.getTestContext();

					String testName = testObj.getName();

					IResultMap testFailedResult = testObj.getFailedTests();
					String failedTestMethodInfo = this.getTestMethodReport(testName, testFailedResult, false, false);
					if (getStringCount("<td", failedTestMethodInfo) > 2)
						retBuf.append(failedTestMethodInfo);

					IResultMap testSkippedResult = testObj.getSkippedTests();
					String skippedTestMethodInfo = this.getTestMethodReport(testName, testSkippedResult, false, true);
					if (getStringCount("<td", skippedTestMethodInfo) > 2)
						retBuf.append(skippedTestMethodInfo);

					IResultMap testPassedResult = testObj.getPassedTests();
					String passedTestMethodInfo = this.getTestMethodReport(testName, testPassedResult, true, false);
					if (getStringCount("<td", passedTestMethodInfo) > 2)
						retBuf.append(passedTestMethodInfo);
				}
			}
		} catch (Exception e) {
			CustomTestNGReporterLog.error(e.getMessage());
		}
		return retBuf.toString();
	}

	private String getTestMethodReport(String testName, IResultMap testResultMap, boolean passedReault,
			boolean skippedResult) {
		StringBuffer retStrBuf = new StringBuffer();

		String resultTitle = testName;


		color = "#3cb353";


		if (skippedResult) {
			resultTitle += " - Skipped ";
			color = "#EEE8AA";
		} else {
			if (!passedReault) {
				resultTitle += " - Failed ";
				color = "#FF4500";
			} else {
				resultTitle += " - Passed ";
				color = "#3cb353";
			}
		}

		retStrBuf.append(
				"<tr bgcolor=" + color + "><td colspan=7><center><b>" + resultTitle + "</b></center></td></tr>");

		Set<ITestResult> testResultSet = testResultMap.getAllResults();
		SortedSet<String> sortedTestsName = new TreeSet<>();
		for (ITestResult testResult : testResultSet) {
			sortedTestsName.add(testResult.getTestClass().getName());
		}
		SortedSet<String> sortedTestsMethodName = new TreeSet<>();
		for (ITestResult testResult : testResultSet) {
			sortedTestsMethodName.add(testResult.getMethod().getMethodName());
		}
		TreeMap<String, CustomTestNgReporterDto> customTestReport = new TreeMap<String, CustomTestNgReporterDto>();
		for (String testsName : sortedTestsName) {
			for (String testMethodName : sortedTestsMethodName) {
				testResultSet.forEach(testResult -> {
					if (testResult.getMethod().getMethodName().equals(testMethodName)
							&& testResult.getTestClass().getName().equals(testsName)) {
						CustomTestNgReporterDto objCustomTestNgReporterDto = new CustomTestNgReporterDto();
						objCustomTestNgReporterDto.setTestMathodName(testResult.getMethod().getMethodName());
						objCustomTestNgReporterDto.setTestClassName(testResult.getTestClass().getName());
						objCustomTestNgReporterDto.setStartTimeMillis(testResult.getStartMillis());
						objCustomTestNgReporterDto.setEndTimeMillis(testResult.getEndMillis());
						objCustomTestNgReporterDto
								.setDeltaMillis(testResult.getEndMillis() - testResult.getStartMillis());
						if(testResult.getThrowable()!=null)
							objCustomTestNgReporterDto.setLog(testResult.getThrowable().toString());
						else
							objCustomTestNgReporterDto.setLog("NA");
						customTestReport.put(testMethodName, objCustomTestNgReporterDto);
					}
				});

			}
		}
		for (String testsName : sortedTestsName) {
			testClassNameFlag = false;
			customTestReport.forEach((testMethod, object) -> {
				countTestClassName = 0;
				customTestReport.forEach((k, v) -> {
					if (v.getTestClassName().equals(testsName))
						countTestClassName++;
				});
				if (object.getTestClassName().equals(testsName)) {
					String testClassName = "";
					String testMethodName = "";
					String startDateStr = "";
					String endDateStr = "";
					String executeTimeStr = "";
					String log="";

					String testClass[]=object.getTestClassName().split(Pattern.quote("."));
					testClassName = testClass[testClass.length-1];

					testMethodName = testMethod;

					startDateStr = this.getTimeInStringFormat(new Date(object.getStartTimeMillis()));

					endDateStr = this.getTimeInStringFormat(new Date(object.getEndTimeMillis()));

					executeTimeStr = this.convertDeltaTimeToString(object.getDeltaMillis());
					log=object.getLog();

					retStrBuf.append("<tr bgcolor=" + color + ">");

					if (!testClassNameFlag) {
						retStrBuf.append("<td rowspan='" + countTestClassName + "'>");
						retStrBuf.append(testClassName);
						retStrBuf.append("</td>");
						testClassNameFlag = true;
					}

					retStrBuf.append("<td>");
					retStrBuf.append(testMethodName);
					retStrBuf.append("</td>");

					retStrBuf.append("<td>");
					retStrBuf.append(startDateStr);
					retStrBuf.append("</td>");

					retStrBuf.append("<td>");
					retStrBuf.append(endDateStr);
					retStrBuf.append("</td>");

					retStrBuf.append("<td>");
					retStrBuf.append(executeTimeStr);
					retStrBuf.append("</td>");

					retStrBuf.append("<td>");
					retStrBuf.append(log);
					retStrBuf.append("</td>");



					retStrBuf.append("</tr>");
				}
			});
		}
		return retStrBuf.toString();
	}

	private String stringArrayToString(String strArr[]) {
		StringBuffer retStrBuf = new StringBuffer();
		if (strArr != null) {
			for (String str : strArr) {
				retStrBuf.append(str);
				retStrBuf.append(" ");
			}
		}
		return retStrBuf.toString();
	}
	
	private int getStringCount(String whatToFind, String content) {
		int M = whatToFind.length();
		int N = content.length();
		int count = 0;

		for (int i = 0; i <= N - M; i++) {
			int j;
			for (j = 0; j < M; j++) {
				if (content.charAt(i + j) != whatToFind.charAt(j)) {
					break;
				}
			}

			if (j == M) {
				count++;
				j = 0;
			}
		}
		return count;
	}		

	private String getCurrentDateForReport() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyy"); 
		LocalDateTime currentDateTime = LocalDateTime.now();
		return dtf.format(currentDateTime).toString();

	}
	
	private void removeOldCustomMosipReport(String outputDirectory) {
		File folder = new File(outputDirectory);
		for (int i = 0; i < folder.listFiles().length; i++) {
			if (folder.listFiles()[i].getName().contains(reportProfixFileName)) {
				if (folder.listFiles()[i].delete()) {
					CustomTestNGReporterLog.info("Old Report has been removed from directory successfuly..!");
				}
			}
		}
	}
	
	private String afterTestReportTittleContent()
	{
		return "</b>\r\n" + 
		"			<br>\r\n" + 
		"			<thead>\r\n" + 
		"				<tr>\r\n" + 
		"					<th>Module Name</th>\r\n" + 
		"					<th># Total Case</th>\r\n" + 
		"					<th># Passed</th>\r\n" + 
		"					<th># Skipped</th>\r\n" + 
		"					<th># Failed</th>\r\n" + 
		"					<th>Start Time<i><legend>(HH:MM:SS)</legend></i></th>\r\n" + 
		"					<th>End Time<i><legend>(HH:MM:SS)</legend></i></th>\r\n" + 
		"					<th>Execute Time<i><legend>(HH:MM:SS)</legend></i></th>\r\n" + 
		"					<th>Build Version</th>\r\n" + 
		"				</tr>\r\n" + 
		"			</thead>";
	}
	
	private String afterTestCaseSummaryContent()
	{
        return "</table>\r\n" + 
        "	</center>\r\n" + 
        "	<br><br><br><br><br><br><br><br><br>\r\n" + 
        "	<center>\r\n" + 
        "		<table id=\"test-summary\">\r\n" + 
        "			<thead>\r\n" + 
        "				<tr>\r\n" + 
        "					<th>API Name</th>\r\n" + 
        "					<th>TestCase</th>\r\n" + 
        "					<th>Start Time<i><legend>(HH:MM:SS)</legend></i></th>\r\n" + 
        "					<th>End Time<i><legend>(HH:MM:SS)</legend></i></th>\r\n" + 
        "					<th>Execution Time<i><legend>(HH:MM:SS:MilliSec)</legend></i></th>\r\n" + 
        "					 <th>Exception Message</th>\r\n" + 
        "				</tr>\r\n" + 
        "			</thead>";
	}
	private String afterTestMethodSummaryContent()
	{
		return "</table>\r\n" + 
		"	</center>\r\n" + 
		"</body>\r\n" + 
		"<br>\r\n" + 
		"<center>\r\n" + 
		"\r\n" + 
		"</center>\r\n" + 
		"</br>\r\n" + 
		"\r\n" + 
		"</html>";
	}

}