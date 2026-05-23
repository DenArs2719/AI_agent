# Technical Design Document

## Overview

This project implements a backend for an inbound Sears Home Services voice diagnostic agent. The system accepts a customer call, collects appliance issue details, guides the caller through safe troubleshooting checks, and schedules a technician when the issue cannot be resolved during the call.

The implementation focuses on Tier 1 and Tier 2 of the assignment:

- Natural inbound voice flow
- Appliance and symptom collection
- Safe diagnostic guidance
- Conversation memory
- Technician database and availability matching
- Appointment proposal and verbal confirmation

## Architecture

The application is a Spring Boot monolith with clear internal boundaries:

```text
Twilio
  -> VoiceWebhookController
  -> VoiceWebhookService
      -> OpenAiDiagnosticService
      -> TroubleshootingScriptService
      -> SchedulingService
      -> AppointmentService
      -> CallSessionRepository
  -> PostgreSQL
```

The voice layer receives Twilio webhooks and returns TwiML. The service layer owns business behavior. Repositories own persistence. This keeps controller code thin and makes the scheduling and appointment logic reusable from both REST APIs and the voice workflow.

If this became a microservice architecture, `VoiceWebhookService` would keep the same orchestration role, but `SchedulingService` and `AppointmentService` would be replaced by API client classes that call separate scheduling and appointment services over HTTP.

## Technology Choices

### Java and Spring Boot

Spring Boot was selected because it provides a productive backend framework for REST APIs, dependency injection, validation, transactions, and persistence. Java also makes the deterministic scheduling and appointment logic straightforward to test.

### PostgreSQL

PostgreSQL stores technician data, availability, appointments, customers, and call-session memory. A relational model fits the scheduling problem well because the core queries involve joins across technicians, service areas, specialties, and availability slots.

### Twilio

Twilio is used for the voice transport. The application returns TwiML with `<Gather input="speech">` and `<Say>`, so Twilio handles the inbound call, speech capture, and text-to-speech. This avoids adding separate speech-to-text and text-to-speech providers for the initial implementation.

### OpenAI Responses API

OpenAI is used for structured dialogue extraction. The model receives the current call-session state and the latest caller speech, then returns JSON updates for fields such as appliance type, symptoms, error codes, ZIP code, and availability.

The model does not make scheduling decisions. It does not pick technicians, create appointments, or decide whether a slot is valid. Those decisions remain deterministic backend logic.

## Conversation State

Each call is represented by a `call_sessions` row keyed by Twilio `CallSid`. The session stores:

- current conversation stage
- appliance type
- symptoms
- error codes
- prior troubleshooting steps
- ZIP code
- customer name
- availability
- proposed technician and slot
- appointment id
- failure metadata

The backend uses a state machine to decide the next required field. This prevents repeated questions and prevents the LLM from skipping required data.

For returning callers, a new Twilio call still creates a new `CallSession` because the new call has a new `CallSid`. After storing the caller phone number, the app looks for the latest non-canceled appointment for that phone number. If found, it links the appointment id to the new session and asks whether the caller is calling about the existing appointment or a new appliance issue.

Conversation stages include:

```text
APPLIANCE_TYPE
SYMPTOMS
ERROR_CODES
TROUBLESHOOTING_STEPS
ZIP_CODE
CUSTOMER_NAME
AVAILABILITY
READY_TO_SCHEDULE
RETURNING_CALLER
SLOT_CONFIRMATION
APPOINTMENT_CONFIRMED
FAILED
ABANDONED
```

## Diagnostic Guidance

The system includes a small safe troubleshooting knowledge map for washer, dryer, refrigerator, dishwasher, oven, and HVAC. The voice flow presents appliance-specific safe checks during the troubleshooting stage.

Examples include:

- confirm power
- check breaker
- confirm door or lid is closed
- check filters
- confirm water supply
- check visible error codes

The system avoids unsafe instructions such as inspecting gas lines, opening panels, or handling internal wiring.

## Scheduling Design

The scheduling schema includes:

- `technicians`
- `technician_service_areas`
- `technician_specialties`
- `availability_slots`
- `customers`
- `appointments`

The scheduler matches by:

1. Customer ZIP code
2. Appliance type
3. Open availability slot within the requested time window

Filtering is pushed into PostgreSQL through a repository query rather than loading all technicians into memory. Java then groups matching slots by technician and builds the response DTO.

Indexes support the main lookup paths:

- service area ZIP code
- technician specialty
- availability slot booked flag and time range
- technician join columns

This is more scalable than scanning all technicians in application memory and better reflects how the design would behave with a larger technician dataset.

## Appointment Flow

When the session has all required fields, the voice workflow calls the scheduling service and proposes a matching slot. The caller must verbally confirm before the appointment is created and confirmed.

Appointment creation:

- creates or reuses the customer record
- links customer, technician, appliance type, issue description, and availability slot
- marks the slot booked
- confirms the appointment after verbal acceptance

## Error Handling

The voice flow is designed to keep the call alive when possible. If OpenAI or another dialogue step fails:

- the current session is not deleted
- the previous stage is stored
- `current_stage` is set to `FAILED`
- error message, timestamp, and count are saved
- Twilio receives valid TwiML asking the caller to repeat the current answer

This preserves useful debugging state and avoids dropping the call with a raw HTTP 500 response.

## Tradeoffs

### Twilio STT/TTS Instead Of Separate Providers

Using Twilio speech gathering and TwiML text-to-speech keeps the initial system smaller and easier to review. A production system might use a dedicated streaming STT/TTS stack for lower latency and more natural turn-taking.

### LLM Extraction Plus Deterministic Backend Logic

The LLM is useful for interpreting natural speech, but deterministic Java code owns the workflow, required fields, scheduling, and appointment creation. This reduces the risk of hallucinated appointments or skipped data.

### Monolith For The Take-Home

A monolith is appropriate for the take-home because it is simpler to run with Docker Compose and easier to review. The internal service boundaries are still shaped so scheduling and appointment logic could become separate services later.

### Hibernate `ddl-auto=update`

The demo uses Hibernate schema updates for speed. In production, schema changes should be managed by Flyway or Liquibase, especially for enum changes, constraints, and indexes.

## Future Improvements

- Add Flyway or Liquibase migrations.
- Add a status endpoint for reviewer health checks.
- Add local parsing for obvious fields such as ZIP code and yes/no to reduce LLM calls.
- Add better date parsing for phrases like "next Tuesday afternoon."
- Add email and image upload flow for Tier 3 visual diagnosis.
- Add call abandonment detection from Twilio status callbacks.
- Add integration tests against PostgreSQL using Testcontainers.
