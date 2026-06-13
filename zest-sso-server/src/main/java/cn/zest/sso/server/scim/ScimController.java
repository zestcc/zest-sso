package cn.zest.sso.server.scim;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/scim/v2")
@RequiredArgsConstructor
public class ScimController {

    private static final String SCIM_JSON = "application/scim+json";

    private final ScimService scimService;

    @GetMapping(value = "/ServiceProviderConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimServiceProviderConfig serviceProviderConfig() {
        return scimService.serviceProviderConfig();
    }

    @GetMapping(value = "/ResourceTypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> resourceTypes() {
        return scimService.resourceTypes();
    }

    @GetMapping(value = "/Schemas", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> schemas() {
        return scimService.schemas();
    }

    @GetMapping(value = "/Users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimListResponse<ScimUserResource> listUsers(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer startIndex,
            @RequestParam(required = false) Integer count) {
        return scimService.listUsers(filter, startIndex, count);
    }

    @GetMapping(value = "/Users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimUserResource getUser(@PathVariable String id) {
        return scimService.getUser(id);
    }

    @PostMapping(value = "/Users", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScimUserResource> createUser(@RequestBody ScimUserResource resource) {
        ScimUserResource created = scimService.createUser(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/Users/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimUserResource replaceUser(@PathVariable String id, @RequestBody ScimUserResource resource) {
        return scimService.replaceUser(id, resource);
    }

    @PatchMapping(value = "/Users/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimUserResource patchUser(@PathVariable String id, @RequestBody Map<String, Object> patchBody) {
        return scimService.patchUser(id, patchBody);
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        scimService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/Groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimListResponse<ScimGroupResource> listGroups(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer startIndex,
            @RequestParam(required = false) Integer count) {
        return scimService.listGroups(filter, startIndex, count);
    }

    @GetMapping(value = "/Groups/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimGroupResource getGroup(@PathVariable String id) {
        return scimService.getGroup(id);
    }

    @PostMapping(value = "/Groups", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScimGroupResource> createGroup(@RequestBody ScimGroupResource resource) {
        ScimGroupResource created = scimService.createGroup(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/Groups/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimGroupResource replaceGroup(@PathVariable String id, @RequestBody ScimGroupResource resource) {
        return scimService.replaceGroup(id, resource);
    }

    @PatchMapping(value = "/Groups/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimGroupResource patchGroup(@PathVariable String id, @RequestBody Map<String, Object> patchBody) {
        return scimService.patchGroup(id, patchBody);
    }

    @DeleteMapping("/Groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        scimService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/Bulk", consumes = {MediaType.APPLICATION_JSON_VALUE, SCIM_JSON}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScimBulkResponse bulk(@RequestBody ScimBulkRequest request) {
        return scimService.processBulk(request);
    }
}
