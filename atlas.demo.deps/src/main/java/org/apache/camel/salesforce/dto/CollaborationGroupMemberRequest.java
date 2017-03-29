/*
 * Salesforce DTO generated by camel-salesforce-maven-plugin
 * Generated on: Mon Mar 02 02:58:34 EST 2015
 */
package org.apache.camel.salesforce.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import org.apache.camel.component.salesforce.api.PicklistEnumConverter;
import org.apache.camel.component.salesforce.api.dto.AbstractSObjectBase;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Salesforce DTO for SObject CollaborationGroupMemberRequest
 */
@XStreamAlias("CollaborationGroupMemberRequest")
public class CollaborationGroupMemberRequest extends AbstractSObjectBase {

    // CollaborationGroupId
    private String CollaborationGroupId;

    @JsonProperty("CollaborationGroupId")
    public String getCollaborationGroupId() {
        return this.CollaborationGroupId;
    }

    @JsonProperty("CollaborationGroupId")
    public void setCollaborationGroupId(String CollaborationGroupId) {
        this.CollaborationGroupId = CollaborationGroupId;
    }

    // RequesterId
    private String RequesterId;

    @JsonProperty("RequesterId")
    public String getRequesterId() {
        return this.RequesterId;
    }

    @JsonProperty("RequesterId")
    public void setRequesterId(String RequesterId) {
        this.RequesterId = RequesterId;
    }

    // ResponseMessage
    private String ResponseMessage;

    @JsonProperty("ResponseMessage")
    public String getResponseMessage() {
        return this.ResponseMessage;
    }

    @JsonProperty("ResponseMessage")
    public void setResponseMessage(String ResponseMessage) {
        this.ResponseMessage = ResponseMessage;
    }

    // Status
    @XStreamConverter(PicklistEnumConverter.class)
    private StatusEnum Status;

    @JsonProperty("Status")
    public StatusEnum getStatus() {
        return this.Status;
    }

    @JsonProperty("Status")
    public void setStatus(StatusEnum Status) {
        this.Status = Status;
    }

}
