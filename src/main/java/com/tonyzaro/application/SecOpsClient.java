// Copyright 2024 Google LLC
//
//     Licensed under the Apache License, Version 2.0 (the "License");
//     you may not use this file except in compliance with the License.
//     You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//     Unless required by applicable law or agreed to in writing, software
//     distributed under the License is distributed on an "AS IS" BASIS,
//     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//     See the License for the specific language governing permissions and
//     limitations under the License.

package com.tonyzaro.application;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.tonyzaro.model.ChronicleLogData;
import com.tonyzaro.model.LogsImportLog;
import com.tonyzaro.model.LogsImportRequest;
import com.tonyzaro.model.LogsImportSource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.Calendar;

public class SecOpsClient {

  @Parameter(names={"--location", "-l"})
  String location;
  @Parameter(names={"--project", "-p"})
  String project;
  @Parameter(names={"--customerid", "-c"})
  String customerid;
  @Parameter(names={"--feedid", "-f"})
  String feedid;
  @Parameter(names={"--forwarderid", "-fw"})
  String forwarderid;
  @Parameter(names={"--logtype", "-lg"})
  String logtype;

  public void getFeedDetails() throws IOException, InterruptedException, URISyntaxException {
    GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();
    AccessToken token = googleCredentials.refreshAccessToken();
    String tokenValue = token.getTokenValue();

    URI secOpsEndopint = new URI(
        "https://" + location
            +"-chronicle.googleapis.com/v1alpha/projects/" + project
            +"/locations/"+ location
            +"/instances/"+ customerid
            +"/feeds/"+ feedid);
    HttpRequest getRequest = HttpRequest.newBuilder()
        .uri(secOpsEndopint)
        .header("Authorization", "Bearer " + tokenValue)
        .GET()
        .build();

    HttpClient httpClient = HttpClient.newHttpClient();

    HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());

    // for demo purposes
    System.out.println("Request details for feed with ID = " + feedid);
    System.out.println("From HTTP GET to Chronicle API fetched these details:");
    System.out.println(getResponse.body());
  }

  public void importNewLogs() throws IOException, InterruptedException, URISyntaxException {
    // timestamp of the two fake log entries
    // https://protobuf.dev/reference/protobuf/google.protobuf/#timestamp
    // timestamp1 used for log_entry_time, the timestamp of the log entry
    Calendar c1 = Calendar.getInstance();
    c1.set(2024, 11, 1, 23, 30, 30);
    Timestamp timestamp1 = Timestamp.newBuilder().setSeconds(c1.getTimeInMillis() / 1000)
        .setNanos((int) ((c1.getTimeInMillis() % 1000) * 1000000)).build();
    // timestamp2 used for collection_time, time after the above when log was collected
    Calendar c2 = Calendar.getInstance();
    c2.set(2024, 11, 1, 23, 31, 30);
    Timestamp timestamp2 = Timestamp.newBuilder().setSeconds(c2.getTimeInMillis() / 1000)
        .setNanos((int) ((c2.getTimeInMillis() % 1000) * 1000000)).build();


    LogsImportRequest request = LogsImportRequest.newBuilder()
        .setInlineSource(
            LogsImportSource.newBuilder()
                .setForwarder("projects/"+project+"/locations/"+location+"/instances/"+customerid+"/forwarders/"+forwarderid)
                .addLogs(LogsImportLog.newBuilder()
                    .setData(ChronicleLogData.newBuilder()
                        .setDim1("dimension1value1")
                        .setMetric1("metric1value1")
                        .build()
                        .toByteString()) //bytes in proto maps to ByteString in Java
                    .setLogEntryTime(timestamp1)
                    .setCollectionTime(timestamp2))
                .addLogs(LogsImportLog.newBuilder()
                    .setData(ChronicleLogData.newBuilder()
                        .setDim1("dimension1value2")
                        .setMetric1("metric1value2")
                        .build()
                        .toByteString()) //bytes in proto maps to ByteString in Java
                    .setLogEntryTime(timestamp1)
                    .setCollectionTime(timestamp2))
                .build()
        ).build();

    GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();
    AccessToken token = googleCredentials.refreshAccessToken();
    String tokenValue = token.getTokenValue();

    URI secOpsEndopint = new URI(
        "https://" + location
            +"-chronicle.googleapis.com/v1alpha/projects/" + project
            +"/locations/"+ location
            +"/instances/"+ customerid
            +"/logTypes/"+ logtype
            +"/logs:import");
    HttpRequest postRequest = HttpRequest.newBuilder()
        .uri(secOpsEndopint)
        .header("Authorization", "Bearer " + tokenValue)
        .header("Content-Type", "application/json")
        //https://protobuf.dev/reference/java/api-docs/com/google/protobuf/util/JsonFormat.html
        .POST(BodyPublishers.ofString(JsonFormat.printer().print(request)))
        .build();

    HttpClient httpClient = HttpClient.newHttpClient();

    HttpResponse<String> getResponse = httpClient.send(postRequest, BodyHandlers.ofString());

    // for demo purposes
    System.out.println("Sent the following JSON to Chronicle API");
    System.out.println(JsonFormat.printer().print(request));
    System.out.println("Received HTTP status code = " + getResponse.statusCode());
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException {
    // parse command line arguments with JCommander
    // https://jcommander.org/#_overview
    SecOpsClient main = new SecOpsClient();
    JCommander.newBuilder()
        .addObject(main)
        .build()
        .parse(args);

    // Execute HTTP GET to fetch feed details from the Google SecOps Chronicle API
    System.out.println("=========================================================");
    System.out.println("=========DEMO 1 HTTP GET to fetch Feed Details===========");
    System.out.println("=========================================================");
    main.getFeedDetails();

    // Execute HTTP POST to import new logs via the Google SecOps Chronicle API
    System.out.println("=========================================================");
    System.out.println("=========DEMO 2 HTTP POST to import log entries==========");
    System.out.println("=========================================================");
    main.importNewLogs();


  }
}
