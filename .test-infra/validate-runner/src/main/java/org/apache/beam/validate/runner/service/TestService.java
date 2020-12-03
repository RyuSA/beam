package org.apache.beam.validate.runner.service;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.beam.validate.runner.model.CaseResult;
import org.apache.beam.validate.runner.model.Configuration;
import org.apache.beam.validate.runner.model.TestResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public interface TestService {

    /**
     * Returns all the tests {@link CaseResult} from test result.
     * @param testResult {@link TestResult}
     * @return set of case result
     */
    default Set<CaseResult> getAllTests(TestResult testResult) {
        Set<CaseResult> caseResults = new HashSet<>();
        Optional.ofNullable(testResult.getSuites()).ifPresent(suites -> suites.forEach(item -> caseResults.addAll(item.getCases())));
        return caseResults;
    }

    /**
     * Returns the jenkins URL for the last successful build.
     *
     * @param job Map of runner an job name retrieved from configuration
     * @param configuration The input configuration
     *
     * @return The URL of last successful job.
     * @throws URISyntaxException
     * @throws IOException
     */
    default URL getUrl(Map<String, String> job, Configuration configuration) throws URISyntaxException, IOException {
        Map<String, Job> jobs = new JenkinsServer(new URI(configuration.getServer())).getJobs();
        JobWithDetails jobWithDetails = jobs.get(job.get(job.keySet().toArray()[0])).details();
        return new URL(jobWithDetails.getLastSuccessfulBuild().getUrl() + configuration.getJsonapi());
    }

    /**
     * Fetches the test name and class name from a test result.
     * @param testResult {@link TestResult}
     * @return set of pair of all testname and test class name
     */
    default Set<Pair<String, String>> getTestNames(TestResult testResult) {
        Set<Pair<String, String>> caseResults = new HashSet<>();
        Optional.ofNullable(testResult.getSuites()).ifPresent(suites -> suites.forEach(item -> item.getCases().forEach(caseResult -> caseResults.add(new ImmutablePair<>(caseResult.getName(), caseResult.getClassName())))));
        return caseResults;
    }

    /**
     * Method find the tests which are not run for a particular runner and add them as the tests
     * which are "NOT RUN" for a particular runner
     *
     * @param testnames Set of Pair of test name and Test ClassName of all the tests which are run across runners.
     * @param map Map of runner and the Tests run for that particular runner
     * @return The JsonObject with each runner and all the tests which are RUN/ NOT RUN.
     */
    default JSONObject process(Set<Pair<String, String>> testnames, HashMap<String, Set<CaseResult>> map) {
        map.forEach((k, v) -> {
            Set<CaseResult> caseResults = v;
            for(Pair<String, String> pair : testnames) {
                boolean found = false;
                for(CaseResult caseResult : caseResults) {
                    if (caseResult.getName().equals(pair.getKey())) {
                        found = true;
                        break;
                    }
                }
                if(found) {
                    found = false;
                } else {
                    caseResults.add(new CaseResult(pair.getValue(), "NOT RUN", pair.getKey()));
                }
            }
        });

        JSONObject jsonMain = new JSONObject();
        map.forEach((k, v) -> {
            JSONArray tests = new JSONArray();
            tests.addAll(v);
            JSONObject jsonOut = new JSONObject();
            jsonOut.put("testCases", tests);
            jsonMain.put(k, jsonOut);
        });
        return jsonMain;
    }
}
