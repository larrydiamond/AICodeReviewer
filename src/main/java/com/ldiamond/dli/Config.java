package com.ldiamond.dli;

import lombok.Data;
import java.util.List;

@Data
public class Config {
    private List<Server> servers;
    private List<Model> models;
    private String judgemodel;
}

@Data
class Server {
    private String name;
    private String apiKey;
    private String organizationId;
    private String projectId;
    private String baseUrl;
}

@Data
class Model {
    private String modelName;
    private String serverName;
}

