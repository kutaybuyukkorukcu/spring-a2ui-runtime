package com.kutaybuyukkorukcu.a2ui.showcase.demo.change;

import java.time.Instant;
import java.util.Objects;

public final class ChangeRequest {

  private final String id;
  private final String service;
  private final String changeType;
  private final String summary;
  private final String notes;
  private final String rollback;
  private final String risk;
  private final ChangeStatus status;
  private final Instant createdAt;
  private final Instant updatedAt;

  private ChangeRequest(Builder builder) {
    this.id = Objects.requireNonNull(builder.id, "id");
    this.service = Objects.requireNonNull(builder.service, "service");
    this.changeType = Objects.requireNonNull(builder.changeType, "changeType");
    this.summary = Objects.requireNonNull(builder.summary, "summary");
    this.notes = builder.notes;
    this.rollback = builder.rollback;
    this.risk = builder.risk;
    this.status = Objects.requireNonNull(builder.status, "status");
    this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt");
  }

  public static Builder builder(String id) {
    return new Builder(id);
  }

  public String id() {
    return id;
  }

  public String service() {
    return service;
  }

  public String changeType() {
    return changeType;
  }

  public String summary() {
    return summary;
  }

  public String notes() {
    return notes;
  }

  public String rollback() {
    return rollback;
  }

  public String risk() {
    return risk;
  }

  public ChangeStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public ChangeRequest withStatus(ChangeStatus newStatus) {
    return copyBuilder().status(newStatus).updatedAt(Instant.now()).build();
  }

  public ChangeRequest withRisk(String newRisk) {
    return copyBuilder().risk(newRisk).build();
  }

  private Builder copyBuilder() {
    return builder(id)
        .service(service)
        .changeType(changeType)
        .summary(summary)
        .notes(notes)
        .rollback(rollback)
        .risk(risk)
        .status(status)
        .createdAt(createdAt)
        .updatedAt(updatedAt);
  }

  public static final class Builder {
    private final String id;
    private String service;
    private String changeType;
    private String summary;
    private String notes;
    private String rollback;
    private String risk;
    private ChangeStatus status = ChangeStatus.DRAFT;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    private Builder(String id) {
      this.id = id;
    }

    public Builder service(String service) {
      this.service = service;
      return this;
    }

    public Builder changeType(String changeType) {
      this.changeType = changeType;
      return this;
    }

    public Builder summary(String summary) {
      this.summary = summary;
      return this;
    }

    public Builder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public Builder rollback(String rollback) {
      this.rollback = rollback;
      return this;
    }

    public Builder risk(String risk) {
      this.risk = risk;
      return this;
    }

    public Builder status(ChangeStatus status) {
      this.status = status;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public ChangeRequest build() {
      return new ChangeRequest(this);
    }
  }
}
