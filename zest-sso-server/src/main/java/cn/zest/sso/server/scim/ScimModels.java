package cn.zest.sso.server.scim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimMeta {

    private String resourceType;
    private String location;
    private String created;
    private String lastModified;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimName {

    private String formatted;
    private String familyName;
    private String givenName;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimEmail {

    private String value;
    private String type;
    private Boolean primary;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimMember {

    private String value;
    private String display;
    private String type;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimUserResource {

    private List<String> schemas;
    private String id;
    private String externalId;
    private String userName;
    private ScimName name;
    private String displayName;
    private List<ScimEmail> emails;
    private Boolean active;
    private ScimMeta meta;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimGroupResource {

    private List<String> schemas;
    private String id;
    private String displayName;
    private List<ScimMember> members;
    private ScimMeta meta;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimListResponse<T> {

    private List<String> schemas;
    private Integer totalResults;
    private Integer startIndex;
    private Integer itemsPerPage;
    private List<T> Resources;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimServiceProviderConfig {

    private List<String> schemas;
    private String documentationUri;
    private Map<String, Object> patch;
    private Map<String, Object> bulk;
    private Map<String, Object> filter;
    private Map<String, Object> changePassword;
    private Map<String, Object> sort;
    private Map<String, Object> etag;
    private List<String> authenticationSchemes;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimErrorResponse {

    private List<String> schemas;
    private String detail;
    private Integer status;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimBulkRequest {

    private List<String> schemas;
    @JsonProperty("Operations")
    private List<ScimBulkOperation> Operations;
    private Integer failOnErrors;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimBulkOperation {

    private String method;
    private String bulkId;
    private String path;
    private Object data;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimBulkResponse {

    private List<String> schemas;
    @JsonProperty("Operations")
    private List<ScimBulkOperationResponse> Operations;
}

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ScimBulkOperationResponse {

    private String bulkId;
    private String method;
    private String location;
    private Integer status;
    private Object response;
}
