package io.visur.plugins.bgsqlitesync.models;

public class MessageWrapper {
    private String MessageId ;
    private String TenantName;
    private String TenantID;
    private String EntityID;
    private String IsAdmin;
    private String EntityType;
    private String EntityName;
    private String DataType;
    private String MessageKind;
    private String Routing;
    private String Type;
    private String UserID;
    private String Payload;
    private String ClientID;
    private String SourceSystem;
    private String AbortSignal;

    public String getMessageId() {
        return MessageId;
    }

    public void setMessageId(String messageId) {
        MessageId = messageId;
    }

    public String getTenantName() {
        return TenantName;
    }

    public void setTenantName(String tenantName) {
        TenantName = tenantName;
    }

    public String getTenantID() {
        return TenantID;
    }

    public void setTenantID(String tenantID) {
        TenantID = tenantID;
    }

    public String getEntityID() {
        return EntityID;
    }

    public void setEntityID(String entityID) {
        EntityID = entityID;
    }

    public String getIsAdmin() {
        return IsAdmin;
    }

    public void setIsAdmin(String isAdmin) {
        IsAdmin = isAdmin;
    }

    public String getEntityType() {
        return EntityType;
    }

    public void setEntityType(String entityType) {
        EntityType = entityType;
    }

    public String getEntityName() {
        return EntityName;
    }

    public void setEntityName(String entityName) {
        EntityName = entityName;
    }

    public String getDataType() {
        return DataType;
    }

    public void setDataType(String dataType) {
        DataType = dataType;
    }

    public String getMessageKind() {
        return MessageKind;
    }

    public void setMessageKind(String messageKind) {
        MessageKind = messageKind;
    }

    public String getRouting() {
        return Routing;
    }

    public void setRouting(String routing) {
        Routing = routing;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }

    public String getPayload() {
        return Payload;
    }

    public void setPayload(String payload) {
        Payload = payload;
    }

    public String getClientID() {
        return ClientID;
    }

    public void setClientID(String clientID) {
        ClientID = clientID;
    }

    public String getSourceSystem() {
        return SourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        SourceSystem = sourceSystem;
    }

    public String getAbortSignal() {
        return AbortSignal;
    }

    public void setAbortSignal(String abortSignal) {
        AbortSignal = abortSignal;
    }
}
