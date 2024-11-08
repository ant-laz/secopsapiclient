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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class SecOpsClient {

  @Parameter(names={"--location", "-l"})
  String location;
  @Parameter(names={"--project", "-p"})
  String project;
  @Parameter(names={"--customerid", "-c"})
  String customerid;
  @Parameter(names={"--feedid", "-f"})
  String feedid;

  public void run() throws IOException, InterruptedException, URISyntaxException {
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

    System.out.printf(getResponse.body());
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

    // Execute calls to the Google SecOps Chronicle API
    main.run();


  }
}
